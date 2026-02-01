package com.streamflow.worker.service;

import com.streamflow.worker.config.WorkerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Transcodes raw video to multi-resolution HLS (240p, 480p, 720p, 1080p) with
 * master playlist. Output: /tmp/{videoAssetId}/packaged/ with master.m3u8 and
 * per-resolution playlists + segments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FFmpegTranscodeService {

    private final WorkerProperties workerProperties;

    private static final List<ResolutionConfig> RESOLUTIONS = List.of(
            new ResolutionConfig("240", 240, "400k"),
            new ResolutionConfig("480", 480, "1000k"),
            new ResolutionConfig("720", 720, "2500k"),
            new ResolutionConfig("1080", 1080, "5000k"));

    /**
     * Transcode input file to HLS in outputDir. Creates subdirs 240/, 480/, 720/,
     * 1080/ and master.m3u8.
     *
     * @param inputPath    path to raw video file
     * @param outputDir    base dir (e.g. /tmp/{videoAssetId}/packaged)
     * @param videoAssetId for logging
     * @return path to master playlist (master.m3u8)
     */
    public Path transcodeToHls(Path inputPath, Path outputDir, UUID videoAssetId)
            throws IOException, InterruptedException {
        if (!Files.isRegularFile(inputPath)) {
            throw new IllegalArgumentException("Input file does not exist: " + inputPath);
        }
        Files.createDirectories(outputDir);

        List<String> masterPlaylistLines = new ArrayList<>();
        masterPlaylistLines.add("#EXTM3U");
        masterPlaylistLines.add("#EXT-X-VERSION:3");

        for (ResolutionConfig res : RESOLUTIONS) {
            Path resDir = outputDir.resolve(res.name);
            Files.createDirectories(resDir);
            Path playlistPath = resDir.resolve("playlist.m3u8");
            String segmentPattern = resDir.resolve("seg_%03d.ts").toString();

            List<String> cmd = List.of(
                    "ffmpeg",
                    "-y",
                    "-i", inputPath.toString(),
                    "-vf", "scale=-2:" + res.height,
                    "-c:v", "libx264",
                    "-b:v", res.bitrate,
                    "-maxrate", res.bitrate,
                    "-bufsize", String.valueOf(Integer.parseInt(res.bitrate.replace("k", "")) * 2) + "k",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-hls_time", "4",
                    "-hls_playlist_type", "vod",
                    "-hls_segment_filename", segmentPattern,
                    "-hls_flags", "independent_segments",
                    playlistPath.toString());

            int exit = runProcess(cmd, outputDir);
            if (exit != 0) {
                throw new RuntimeException("FFmpeg failed for resolution " + res.name + " (exit " + exit + ")");
            }
            int width = (res.height * 16) / 9;
            masterPlaylistLines.add("#EXT-X-STREAM-INF:BANDWIDTH=" + bandwidthKbps(res.bitrate) + ",RESOLUTION=" + width
                    + "x" + res.height);
            masterPlaylistLines.add(res.name + "/playlist.m3u8");
        }

        Path masterPath = outputDir.resolve("master.m3u8");
        Files.writeString(masterPath, String.join("\n", masterPlaylistLines), StandardCharsets.UTF_8);
        log.info("Transcoded {} to HLS at {} (master.m3u8)", videoAssetId, outputDir);
        return masterPath;
    }

    private static int bandwidthKbps(String bitrate) {
        String num = bitrate.replace("k", "");
        return Integer.parseInt(num) * 1000; // BANDWIDTH in bps for m3u8
    }

    private int runProcess(List<String> cmd, Path workDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd)
                .directory(workDir.toFile())
                .redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("ffmpeg: {}", line);
            }
        }
        boolean finished = p.waitFor(workerProperties.getFfmpegTimeoutSeconds(), TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new RuntimeException("FFmpeg timed out after " + workerProperties.getFfmpegTimeoutSeconds() + "s");
        }
        return p.exitValue();
    }

    private static final class ResolutionConfig {
        final String name;
        final int height;
        final String bitrate;

        ResolutionConfig(String name, int height, String bitrate) {
            this.name = name;
            this.height = height;
            this.bitrate = bitrate;
        }
    }
}
