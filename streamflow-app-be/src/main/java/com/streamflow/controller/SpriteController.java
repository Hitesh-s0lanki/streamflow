package com.streamflow.controller;

import com.streamflow.dto.RegisterSpriteFrameRequest;
import com.streamflow.dto.SpriteFrameResponse;
import com.streamflow.service.VideoAssetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Sprite frame metadata for frame-accurate seek preview.
 * POST /api/sprites/{spriteSheetId}/frames — register frame metadata.
 * GET /api/sprites/{spriteSheetId}/frames — get frames ordered by frameIndex
 * ASC.
 */
@RestController
@RequestMapping("/api/sprites")
public class SpriteController {

    private final VideoAssetService videoAssetService;

    public SpriteController(VideoAssetService videoAssetService) {
        this.videoAssetService = videoAssetService;
    }

    @PostMapping("/{spriteSheetId}/frames")
    public ResponseEntity<SpriteFrameResponse> addFrame(@PathVariable UUID spriteSheetId,
            @Valid @RequestBody RegisterSpriteFrameRequest request) {
        SpriteFrameResponse created = videoAssetService.addSpriteFrame(spriteSheetId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{spriteSheetId}/frames")
    public ResponseEntity<List<SpriteFrameResponse>> getFrames(@PathVariable UUID spriteSheetId) {
        return ResponseEntity.ok(videoAssetService.getSpriteFrames(spriteSheetId));
    }
}
