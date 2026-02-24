package com.streamflow.service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.streamflow.entity.SpriteSheet;
import com.streamflow.entity.VideoAsset;
import com.streamflow.exception.VideoProcessingException;
import com.streamflow.repository.SpriteSheetRepository;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SpriteSheetService {

    private static final String FFMPEG_CMD = "ffmpeg";
    private static final String THUMB_PREFIX = "thumb_";
    private static final String THUMB_SUFFIX = ".jpg";

    @Value("${streamflow.sprite.interval-seconds:10}")
    private int intervalSeconds;

    @Value("${streamflow.sprite.thumb-width:160}")
    private int thumbWidth;

    @Value("${streamflow.sprite.thumb-height:90}")
    private int thumbHeight;

    @Value("${streamflow.sprite.max-total-frames:100000}")
    private int maxTotalFrames;

    @Value("${streamflow.sprite.frames-per-sheet:100}")
    private int framesPerSheet;

    @Value("${streamflow.sprite.columns:10}")
    private int columns;

    private final SpriteSheetRepository spriteSheetRepository;
    private final S3StorageService s3StorageService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    // Build sprite image from thumbnail files
    public BufferedImage buildSpriteImage(List<Path> thumbFiles, int cols, int rows, int thumbWidth, int thumbHeight)
            throws IOException {
        BufferedImage sprite = new BufferedImage(cols * thumbWidth, rows * thumbHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sprite.createGraphics();
        try {
            for (int i = 0; i < thumbFiles.size(); i++) {
                BufferedImage img = ImageIO.read(thumbFiles.get(i).toFile());
                if (img == null)
                    continue;
                int x = (i % cols) * thumbWidth;
                int y = (i / cols) * thumbHeight;
                g.drawImage(img, x, y, thumbWidth, thumbHeight, null);
            }
        } finally {
            g.dispose();
        }
        return sprite;
    }

    // Build S3 key for sprite sheet
    public String buildSpriteS3Key(UUID videoAssetId, int sheetIndex) {
        String prefix = StringUtils.hasText(s3StorageService.getSpritesPrefix())
                ? s3StorageService.getSpritesPrefix()
                : S3StorageService.DEFAULT_SPRITES_PREFIX;
        return prefix + videoAssetId + "-" + sheetIndex + ".jpg";
    }

    // Upload sprite image to S3
    public void uploadSpriteToS3(Path spritePath, String key) throws IOException {
        long size = Files.size(spritePath);
        try (InputStream is = Files.newInputStream(spritePath)) {
            s3StorageService.upload(key, is, size, "image/jpeg", null);
        }
    }

    // Calculate target frames based on video duration
    public int calculateTargetFrames(int durationSeconds) {
        return durationSeconds > 0
                ? Math.min(maxTotalFrames, (int) Math.ceil((double) durationSeconds / intervalSeconds))
                : maxTotalFrames;
    }

    // Calculate total number of sprite sheets needed
    public int calculateTotalSheets(int totalFrames) {
        return (int) Math.ceil((double) totalFrames / framesPerSheet);
    }

    // Calculate number of rows for a sprite sheet
    public int calculateRows(int frameCount) {
        return (int) Math.ceil((double) frameCount / columns);
    }

    // Get video duration in seconds using ffprobe
    public int getVideoDurationSeconds(Path videoPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    videoPath.toAbsolutePath().toString());

            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new VideoProcessingException(
                        "ffprobe failed with exit code " + exitCode + (output.isEmpty() ? "" : ": " + output));
            }
            if (output.isEmpty()) {
                throw new VideoProcessingException("ffprobe produced no output; unable to read video duration");
            }

            double durationDouble = Double.parseDouble(output);
            if (durationDouble <= 0 || !Double.isFinite(durationDouble)) {
                throw new VideoProcessingException("ffprobe returned invalid duration: " + output);
            }
            return (int) Math.ceil(durationDouble);
        } catch (IOException e) {
            throw new VideoProcessingException(
                    "ffprobe is not available or failed to run. Install FFmpeg (e.g. brew install ffmpeg). "
                            + e.getMessage(),
                    e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VideoProcessingException("ffprobe was interrupted while reading video duration", e);
        } catch (NumberFormatException e) {
            throw new VideoProcessingException("ffprobe returned non-numeric duration", e);
        }
    }

    // Extract frames from video using FFmpeg
    public void extractFrames(Path videoPath, Path outputDir, int maxFrames) throws IOException, InterruptedException {
        String filter = String.format("fps=1/%d,scale=%d:%d", intervalSeconds, thumbWidth, thumbHeight);
        List<String> command = new ArrayList<>(List.of(
                FFMPEG_CMD,
                "-y",
                "-i", videoPath.toAbsolutePath().toString(),
                "-vf", filter,
                "-vframes", String.valueOf(maxFrames),
                THUMB_PREFIX + "%03d" + THUMB_SUFFIX));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(outputDir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String out = new String(process.getInputStream().readAllBytes());
        int exit = process.waitFor();
        if (exit != 0) {
            log.warn("FFmpeg exit code {} for sprite extraction. Output: {}", exit, out);
            throw new IOException("FFmpeg failed with exit code " + exit + ": "
                    + (out.length() > 500 ? out.substring(0, 500) + "..." : out));
        }
    }

    // List and sort thumbnail files from directory
    public List<Path> listThumbFiles(Path thumbsDir) throws IOException {
        try (Stream<Path> stream = Files.list(thumbsDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith(THUMB_PREFIX)
                            && p.getFileName().toString().endsWith(THUMB_SUFFIX))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        }
    }

    // Get sprite sheet configuration values
    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public int getThumbWidth() {
        return thumbWidth;
    }

    public int getThumbHeight() {
        return thumbHeight;
    }

    public int getMaxTotalFrames() {
        return maxTotalFrames;
    }

    public int getFramesPerSheet() {
        return framesPerSheet;
    }

    public int getColumns() {
        return columns;
    }

    // Process and save sprite sheets with multi-threading
    public void processAndSaveSpriteSheets(
            List<Path> thumbFiles,
            VideoAsset videoAsset,
            Path workDir) throws IOException {

        UUID videoAssetId = videoAsset.getId();
        int totalFrames = thumbFiles.size();

        // Delete existing sprite sheets for this video asset
        List<SpriteSheet> existingSheets = spriteSheetRepository
                .findAllByVideoAssetIdOrderBySheetIndexAsc(videoAssetId);
        spriteSheetRepository.deleteAll(existingSheets);

        // Calculate number of sheets needed
        int totalSheets = calculateTotalSheets(totalFrames);

        // Create list to hold all sprite sheet entities
        List<SpriteSheet> spriteSheets = new ArrayList<>(totalSheets);

        // Process sheets in parallel
        List<CompletableFuture<SpriteSheet>> futures = IntStream.range(0, totalSheets)
                .mapToObj(sheetIndex -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return processSingleSheet(
                                sheetIndex,
                                thumbFiles,
                                videoAsset,
                                workDir,
                                totalFrames);
                    } catch (Exception e) {
                        log.error("Error processing sprite sheet {} for video asset {}: {}", 
                                sheetIndex, videoAssetId, e.getMessage(), e);
                        throw new RuntimeException("Failed to process sprite sheet " + sheetIndex, e);
                    }
                }, executorService))
                .toList();

        // Wait for all sheets to be processed and collect results
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Collect all sprite sheets
        for (CompletableFuture<SpriteSheet> future : futures) {
            try {
                spriteSheets.add(future.get());
            } catch (Exception e) {
                log.error("Error getting sprite sheet result: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to get sprite sheet result", e);
            }
        }

        // Save all sprite sheets to database in batch
        spriteSheetRepository.saveAll(spriteSheets);
        log.info("Successfully processed and saved {} sprite sheets for video asset {}", 
                spriteSheets.size(), videoAssetId);
    }

    // Shutdown executor service on bean destruction
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        log.info("SpriteSheetService executor service shut down");
    }

    // Process a single sprite sheet
    private SpriteSheet processSingleSheet(
            int sheetIndex,
            List<Path> thumbFiles,
            VideoAsset videoAsset,
            Path workDir,
            int totalFrames) throws IOException {

        int start = sheetIndex * framesPerSheet;
        int end = Math.min(start + framesPerSheet, totalFrames) - 1;
        int count = end - start + 1;
        List<Path> chunk = thumbFiles.subList(start, end + 1);
        int rows = calculateRows(count);

        // Build sprite image
        BufferedImage spriteImage = buildSpriteImage(chunk, columns, rows, thumbWidth, thumbHeight);

        // Save sprite image to temporary file
        Path spritePath = workDir.resolve("sprite-" + sheetIndex + ".jpg");
        ImageIO.write(spriteImage, "jpg", spritePath.toFile());

        // Upload to S3
        String spriteS3Key = buildSpriteS3Key(videoAsset.getId(), sheetIndex);
        uploadSpriteToS3(spritePath, spriteS3Key);

        // Create sprite sheet entity
        SpriteSheet spriteSheet = new SpriteSheet();
        spriteSheet.setVideoAsset(videoAsset);
        spriteSheet.setSheetIndex(sheetIndex);
        spriteSheet.setStartFrame(start);
        spriteSheet.setEndFrame(end);
        spriteSheet.setFramesCount(count);
        spriteSheet.setSpriteS3Key(spriteS3Key);
        spriteSheet.setIntervalSeconds(intervalSeconds);
        spriteSheet.setThumbWidth(thumbWidth);
        spriteSheet.setThumbHeight(thumbHeight);
        spriteSheet.setColumnsCount(columns);
        spriteSheet.setRowsCount(rows);

        return spriteSheet;
    }

    // Delete file quietly, ignoring errors
    public void deleteQuietly(Path path) {
        if (path == null)
            return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete temp file {}: {}", path, e.getMessage());
        }
    }

    // Delete directory recursively, ignoring errors
    public void deleteDirQuietly(Path dir) {
        if (dir == null || !Files.isDirectory(dir))
            return;
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path p : stream.toList()) {
                if (Files.isDirectory(p)) {
                    deleteDirQuietly(p);
                } else {
                    Files.deleteIfExists(p);
                }
            }
            Files.deleteIfExists(dir);
        } catch (IOException e) {
            log.warn("Could not delete temp dir {}: {}", dir, e.getMessage());
        }
    }

    /**
     * Processes a video file on disk and generates sprite sheets.
     * The caller is responsible for cleaning up {@code videoFilePath}.
     */
    public void processVideoAndGenerateSprites(VideoAsset videoAsset, Path videoFilePath) throws Exception {
        Path workDir = null;

        try {
            workDir = Files.createTempDirectory("streamflow-sprite-");
            Path thumbsDir = workDir.resolve("thumbs");
            Files.createDirectories(thumbsDir);

            int durationSeconds = getVideoDurationSeconds(videoFilePath);
            int targetFrames = calculateTargetFrames(durationSeconds);

            if (targetFrames < 1)
                throw new VideoProcessingException("Target frames is less than 1");

            extractFrames(videoFilePath, thumbsDir, targetFrames);

            List<Path> thumbFiles = listThumbFiles(thumbsDir);

            if (thumbFiles.isEmpty()) {
                throw new VideoProcessingException("FFmpeg produced no thumbnails.");
            }

            processAndSaveSpriteSheets(thumbFiles, videoAsset, workDir);
        } finally {
            deleteDirQuietly(workDir);
        }
    }
}
