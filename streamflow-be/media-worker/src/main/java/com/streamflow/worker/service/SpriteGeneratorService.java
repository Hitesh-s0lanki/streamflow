package com.streamflow.worker.service;

import com.streamflow.worker.config.WorkerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Extracts frames every N seconds, resizes to thumbnail size, combines into
 * sprite sheets, and returns metadata for DB persistence. Output sprites
 * uploaded by caller to S3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpriteGeneratorService {

    private final WorkerProperties workerProperties;

    private static final int COLS = 5;
    private static final int ROWS = 5;
    private static final int FRAMES_PER_SHEET = COLS * ROWS;

    /**
     * Result for one sprite sheet: local file path and metadata for
     * SpriteSheet + SpriteFrameMetadata.
     */
    public record SpriteSheetResult(
            Path localPath,
            int startTimeSeconds,
            int endTimeSeconds,
            int columns,
            int rows,
            int thumbnailWidth,
            int thumbnailHeight,
            int intervalSeconds,
            List<FrameMeta> frameMetadata) {
        public record FrameMeta(int frameIndex, int timeOffsetSeconds, int xPosition, int yPosition, int width,
                int height) {
        }
    }

    /**
     * Generate sprite sheets from video. Extracts frames at intervalSeconds,
     * tiles into 5x5 sheets, writes to outputDir. Returns one result per sheet.
     */
    public List<SpriteSheetResult> generateSprites(Path inputPath, Path outputDir, int durationSeconds)
            throws IOException, InterruptedException {
        int interval = workerProperties.getSpriteIntervalSeconds();
        int w = workerProperties.getSpriteThumbWidth();
        int h = workerProperties.getSpriteThumbHeight();

        int numFrames = Math.max(1, (durationSeconds + interval - 1) / interval);
        Path framesDir = outputDir.resolve("frames");
        Files.createDirectories(framesDir);

        // Extract frames with FFmpeg: one frame every intervalSeconds
        List<String> ffmpegCmd = List.of(
                "ffmpeg",
                "-y",
                "-i", inputPath.toString(),
                "-vf",
                "scale=" + w + ":" + h + ":force_original_aspect_ratio=decrease,pad=" + w + ":" + h
                        + ":(ow-iw)/2:(oh-ih)/2",
                "-vsync", "0",
                "-r", String.format("1/%d", interval),
                "-frames:v", String.valueOf(numFrames),
                framesDir.resolve("frame_%04d.jpg").toString());
        ProcessBuilder pb = new ProcessBuilder(ffmpegCmd).directory(outputDir.toFile()).redirectErrorStream(true);
        Process p = pb.start();
        p.getInputStream().transferTo(System.err); // consume to avoid blocking
        boolean finished = p.waitFor(300, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new RuntimeException("FFmpeg sprite frames timed out");
        }
        if (p.exitValue() != 0) {
            throw new RuntimeException("FFmpeg sprite frames failed with exit " + p.exitValue());
        }

        List<SpriteSheetResult> results = new ArrayList<>();
        List<Path> frameFiles = Files.list(framesDir)
                .filter(f -> f.toString().endsWith(".jpg"))
                .sorted()
                .toList();

        for (int sheetIndex = 0; sheetIndex * FRAMES_PER_SHEET < frameFiles.size(); sheetIndex++) {
            int from = sheetIndex * FRAMES_PER_SHEET;
            int to = Math.min(from + FRAMES_PER_SHEET, frameFiles.size());
            List<Path> sheetFrames = frameFiles.subList(from, to);
            int startTime = from * interval;
            int endTime = Math.min((to) * interval, durationSeconds);

            BufferedImage sheet = new BufferedImage(COLS * w, ROWS * h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = sheet.createGraphics();
            List<SpriteSheetResult.FrameMeta> metas = new ArrayList<>();
            for (int i = 0; i < sheetFrames.size(); i++) {
                int col = i % COLS;
                int row = i / COLS;
                int x = col * w;
                int y = row * h;
                BufferedImage thumb = ImageIO.read(sheetFrames.get(i).toFile());
                if (thumb != null) {
                    g.drawImage(thumb, x, y, w, h, null);
                }
                metas.add(new SpriteSheetResult.FrameMeta(i, startTime + i * interval, x, y, w, h));
            }
            g.dispose();

            String filename = "sprite_" + String.format("%03d", sheetIndex + 1) + ".jpg";
            Path spritePath = outputDir.resolve(filename);
            ImageIO.write(sheet, "jpg", spritePath.toFile());

            results.add(new SpriteSheetResult(
                    spritePath,
                    startTime,
                    endTime,
                    COLS,
                    ROWS,
                    w,
                    h,
                    interval,
                    metas));
        }

        // Cleanup frames dir
        for (Path f : frameFiles) {
            Files.deleteIfExists(f);
        }
        Files.deleteIfExists(framesDir);
        log.info("Generated {} sprite sheets for {} frames", results.size(), numFrames);
        return results;
    }

    /**
     * Get video duration in seconds using ffprobe.
     */
    public int getDurationSeconds(Path inputPath) throws IOException, InterruptedException {
        List<String> cmd = List.of(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                inputPath.toString());
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes());
        boolean finished = p.waitFor(10, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new RuntimeException("ffprobe timed out");
        }
        if (p.exitValue() != 0) {
            throw new RuntimeException("ffprobe failed: " + out);
        }
        double sec = Double.parseDouble(out.trim());
        return (int) Math.ceil(sec);
    }
}
