package com.streamflow.service;

import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.VideoVariant;
import com.streamflow.exception.VideoProcessingException;
import com.streamflow.repository.VideoAssetRepository;
import com.streamflow.repository.VideoVariantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Transcodes a raw video file into HLS adaptive bitrate streams using FFmpeg,
 * uploads the resulting files (master playlist, variant playlists, .ts segments)
 * to S3, and persists VideoVariant metadata.
 *
 * <h3>Performance optimizations</h3>
 * <ul>
 *   <li>Single-pass FFmpeg: decodes the source once and encodes all variants
 *       simultaneously via filter_complex + split (halves decode time).</li>
 *   <li>ultrafast x264 preset: trades file size for dramatically faster encoding.</li>
 *   <li>Parallel S3 uploads: 16 concurrent threads instead of sequential.</li>
 *   <li>Larger default segment duration (10 s → fewer files → less I/O).</li>
 * </ul>
 *
 * S3 layout produced:
 * <pre>
 *   hls/{videoAssetId}/master.m3u8
 *   hls/{videoAssetId}/360p/playlist.m3u8
 *   hls/{videoAssetId}/360p/segment000.ts
 *   hls/{videoAssetId}/720p/playlist.m3u8
 *   hls/{videoAssetId}/720p/segment000.ts
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HlsTranscodingService {

    private static final String FFMPEG_CMD = "ffmpeg";
    private static final String MASTER_PLAYLIST = "master.m3u8";
    private static final String VARIANT_PLAYLIST = "playlist.m3u8";
    private static final String HLS_S3_PREFIX = "hls/";
    private static final int UPLOAD_PARALLELISM = 16;

    private final S3StorageService s3StorageService;
    private final VideoAssetRepository videoAssetRepository;
    private final VideoVariantRepository videoVariantRepository;

    @Value("${streamflow.hls.segment-duration:10}")
    private int segmentDuration;

    private record VariantPreset(
            String label,
            int width,
            int height,
            int videoBitrateKbps,
            int audioBitrateKbps,
            String codec,
            int sortOrder) {}

    private static final List<VariantPreset> PRESETS = List.of(
            new VariantPreset("360p", 640, 360, 800, 96, "h264", 1),
            new VariantPreset("720p", 1280, 720, 2800, 128, "h264", 2)
    );

    /**
     * Full HLS transcoding pipeline:
     * <ol>
     *   <li>Create a temp work directory</li>
     *   <li>Run FFmpeg (single-pass, all variants in one invocation)</li>
     *   <li>Generate the master playlist that references variants with relative paths</li>
     *   <li>Upload everything to S3 in parallel</li>
     *   <li>Save VideoVariant entities and set manifestUrl on VideoAsset</li>
     * </ol>
     */
    public void transcode(VideoAsset videoAsset, Path videoFilePath, int durationSeconds) {
        UUID videoAssetId = videoAsset.getId();
        Path workDir = null;
        long pipelineStart = System.currentTimeMillis();

        try {
            workDir = Files.createTempDirectory("streamflow-hls-");
            log.info("HLS transcode started: videoAssetId={}, duration={}s, workDir={}",
                    videoAssetId, durationSeconds, workDir);

            long t0 = System.currentTimeMillis();
            runFfmpegSinglePass(videoFilePath, workDir);
            log.info("[TIMING] FFmpeg single-pass encode: {}ms  (videoAsset={})",
                    System.currentTimeMillis() - t0, videoAssetId);

            writeMasterPlaylist(workDir);

            long t1 = System.currentTimeMillis();
            String manifestS3Key = uploadHlsToS3Parallel(videoAssetId, workDir);
            log.info("[TIMING] Parallel S3 upload: {}ms  (videoAsset={})",
                    System.currentTimeMillis() - t1, videoAssetId);

            videoAsset.setManifestUrl(manifestS3Key);
            videoAsset.setDurationSeconds(durationSeconds);
            videoAssetRepository.save(videoAsset);

            saveVariantEntities(videoAsset);

            log.info("HLS transcode completed: videoAssetId={}, manifestUrl={}, totalTime={}ms",
                    videoAssetId, manifestS3Key, System.currentTimeMillis() - pipelineStart);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new VideoProcessingException(
                    "HLS transcoding failed for videoAsset " + videoAssetId + ": " + e.getMessage(), e);
        } finally {
            deleteDirQuietly(workDir);
        }
    }

    /**
     * Single FFmpeg invocation: decodes the source once, splits the video stream,
     * scales each copy, and encodes all variants simultaneously.
     * Uses ultrafast preset and -threads 0 (auto-detect) for maximum speed.
     */
    private void runFfmpegSinglePass(Path videoFilePath, Path workDir) throws IOException, InterruptedException {
        for (VariantPreset preset : PRESETS) {
            Files.createDirectories(workDir.resolve(preset.label()));
        }

        List<String> cmd = new ArrayList<>();
        cmd.addAll(List.of(
                FFMPEG_CMD,
                "-y",
                "-threads", "0",
                "-i", videoFilePath.toAbsolutePath().toString()
        ));

        // filter_complex: split → scale each variant (decode once)
        StringBuilder fc = new StringBuilder();
        fc.append("[0:v]split=").append(PRESETS.size());
        for (int i = 0; i < PRESETS.size(); i++) {
            fc.append("[vin").append(i).append(']');
        }
        fc.append("; ");
        for (int i = 0; i < PRESETS.size(); i++) {
            VariantPreset p = PRESETS.get(i);
            fc.append("[vin").append(i).append("]scale=")
              .append(p.width()).append(':').append(p.height())
              .append("[v").append(i).append(']');
            if (i < PRESETS.size() - 1) fc.append("; ");
        }
        cmd.addAll(List.of("-filter_complex", fc.toString()));

        for (int i = 0; i < PRESETS.size(); i++) {
            VariantPreset p = PRESETS.get(i);
            Path variantDir = workDir.resolve(p.label());
            cmd.addAll(List.of(
                    "-map", "[v" + i + "]",
                    "-map", "0:a",
                    "-c:v", "libx264",
                    "-b:v", p.videoBitrateKbps() + "k",
                    "-maxrate", (int) (p.videoBitrateKbps() * 1.1) + "k",
                    "-bufsize", (p.videoBitrateKbps() * 2) + "k",
                    "-preset", "ultrafast",
                    "-profile:v", "main",
                    "-c:a", "aac",
                    "-b:a", p.audioBitrateKbps() + "k",
                    "-ac", "2",
                    "-f", "hls",
                    "-hls_time", String.valueOf(segmentDuration),
                    "-hls_list_size", "0",
                    "-hls_segment_filename",
                    variantDir.resolve("segment%03d.ts").toAbsolutePath().toString(),
                    variantDir.resolve(VARIANT_PLAYLIST).toAbsolutePath().toString()
            ));
        }

        log.info("Running single-pass FFmpeg: {} variants, preset=ultrafast, segment={}s",
                PRESETS.size(), segmentDuration);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Drain stdout/stderr in a virtual thread to prevent pipe buffer deadlock
        StringBuilder outputTail = new StringBuilder();
        Thread drainer = Thread.ofVirtual().start(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (outputTail.length() > 4000) {
                        outputTail.delete(0, outputTail.length() - 2000);
                    }
                    outputTail.append(line).append('\n');
                }
            } catch (IOException ignored) {}
        });

        int exitCode = process.waitFor();
        drainer.join(5_000);

        if (exitCode != 0) {
            String tail = outputTail.length() > 1000
                    ? outputTail.substring(outputTail.length() - 1000)
                    : outputTail.toString();
            throw new VideoProcessingException(
                    "FFmpeg single-pass transcode failed (exit " + exitCode + "): " + tail);
        }

        log.info("All {} variants transcoded in single pass", PRESETS.size());
    }

    private void writeMasterPlaylist(Path workDir) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("#EXTM3U\n");
        sb.append("#EXT-X-VERSION:3\n");

        for (VariantPreset p : PRESETS) {
            int bandwidth = (p.videoBitrateKbps() + p.audioBitrateKbps()) * 1000;
            sb.append(String.format(
                    "#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%dx%d\n",
                    bandwidth, p.width(), p.height()));
            sb.append(p.label()).append("/").append(VARIANT_PLAYLIST).append("\n");
        }

        Path masterPath = workDir.resolve(MASTER_PLAYLIST);
        Files.writeString(masterPath, sb.toString());
        log.info("Written master playlist: {}", masterPath);
    }

    /**
     * Uploads all HLS files to S3 with {@value #UPLOAD_PARALLELISM} concurrent
     * threads. Returns the S3 key of the master manifest.
     */
    private String uploadHlsToS3Parallel(UUID videoAssetId, Path workDir) throws IOException, InterruptedException {
        String s3Prefix = HLS_S3_PREFIX + videoAssetId + "/";

        List<Path> files = new ArrayList<>();
        Files.walkFileTree(workDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                files.add(file);
                return FileVisitResult.CONTINUE;
            }
        });

        int total = files.size();
        log.info("Uploading {} HLS files to S3 (parallelism={})", total, UPLOAD_PARALLELISM);

        ExecutorService executor = Executors.newFixedThreadPool(UPLOAD_PARALLELISM);
        AtomicInteger uploaded = new AtomicInteger(0);

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>(total);
            for (Path file : files) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        String relativePath = workDir.relativize(file).toString();
                        String s3Key = s3Prefix + relativePath;
                        String contentType = guessContentType(file.getFileName().toString());
                        long size = Files.size(file);
                        try (InputStream is = Files.newInputStream(file)) {
                            s3StorageService.upload(s3Key, is, size, contentType, null);
                        }
                        int done = uploaded.incrementAndGet();
                        if (done % 50 == 0 || done == total) {
                            log.info("S3 upload progress: {}/{}", done, total);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }, executor));
            }

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UncheckedIOException uio) throw uio.getCause();
            if (cause instanceof IOException io) throw io;
            throw new IOException("S3 parallel upload failed", cause);
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                executor.shutdownNow();
                log.warn("S3 upload executor did not terminate in 5 min; forced shutdown");
            }
        }

        String manifestKey = s3Prefix + MASTER_PLAYLIST;
        log.info("Uploaded {} HLS files to S3: prefix={}", total, s3Prefix);
        return manifestKey;
    }

    private void saveVariantEntities(VideoAsset videoAsset) {
        videoVariantRepository.deleteAllByVideoAssetId(videoAsset.getId());

        List<VideoVariant> variants = new ArrayList<>();
        for (VariantPreset p : PRESETS) {
            VideoVariant v = new VideoVariant();
            v.setVideoAsset(videoAsset);
            v.setResolution(p.label());
            v.setBitrateKbps(p.videoBitrateKbps());
            v.setCodec(p.codec());
            v.setSegmentPath(HLS_S3_PREFIX + videoAsset.getId() + "/" + p.label() + "/");
            v.setSortOrder(p.sortOrder());
            variants.add(v);
        }

        videoVariantRepository.saveAll(variants);
        log.info("Saved {} VideoVariant entities for videoAsset={}", variants.size(), videoAsset.getId());
    }

    private static String guessContentType(String filename) {
        if (filename.endsWith(".m3u8")) return "application/vnd.apple.mpegurl";
        if (filename.endsWith(".ts")) return "video/MP2T";
        return "application/octet-stream";
    }

    private void deleteDirQuietly(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        } catch (IOException e) {
            log.warn("Could not clean up HLS work dir {}: {}", dir, e.getMessage());
        }
    }
}
