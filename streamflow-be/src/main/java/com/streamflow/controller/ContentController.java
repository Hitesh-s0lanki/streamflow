package com.streamflow.controller;

import com.streamflow.dto.CreateContentRequest;
import com.streamflow.service.ContentService;
import com.streamflow.service.VideoProcessingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
@Slf4j
public class ContentController {

    private final ContentService contentService;
    private final VideoProcessingService videoProcessingService;

    @GetMapping
    public ResponseEntity<?> getAllContent() {
        return contentService.getAllContent();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getContentById(@PathVariable UUID id) {
        return contentService.getContentById(id);
    }

    @PostMapping
    public ResponseEntity<?> createContent(@Valid @RequestBody CreateContentRequest request) {
        return contentService.createContent(request);
    }

    @PostMapping(value = "/{id}/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAssets(
            @PathVariable UUID id,
            @RequestParam("poster") MultipartFile poster,
            @RequestParam("thumbnail") MultipartFile thumbnail) {
        return contentService.uploadAssets(id, poster, thumbnail);
    }

    @PostMapping(value = "/{id}/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVideo(
            @PathVariable UUID id,
            @RequestParam("video") MultipartFile video) {
        return contentService.uploadVideo(id, video);
    }

    @GetMapping("/{id}/video/status")
    public ResponseEntity<?> getVideoUploadStatus(@PathVariable UUID id) {
        return contentService.getVideoUploadStatus(id);
    }

    @PostMapping(value = "/{id}/video/retry", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> retryVideoUpload(
            @PathVariable UUID id,
            @RequestParam("video") MultipartFile video) {
        return contentService.retryVideoUpload(id, video);
    }

    @DeleteMapping("/{id}/video")
    public ResponseEntity<?> abortVideoUpload(@PathVariable UUID id) {
        return contentService.abortVideoUpload(id);
    }

    /**
     * Triggers sprite-sheet generation for the already-uploaded video.
     * No request body; video is read from S3 (raw). HLS is done only in the upload API.
     */
    @PostMapping("/{id}/video/process")
    public ResponseEntity<?> processVideo(@PathVariable UUID id) {
        return videoProcessingService.processMovieVideo(id);
    }

    @GetMapping("/{id}/video/process/status")
    public ResponseEntity<?> getVideoProcessingStatus(@PathVariable UUID id) {
        return videoProcessingService.getProcessingStatus(id);
    }
}
