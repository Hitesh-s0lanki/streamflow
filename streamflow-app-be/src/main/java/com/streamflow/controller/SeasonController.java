package com.streamflow.controller;

import com.streamflow.dto.CreateEpisodeRequest;
import com.streamflow.dto.EpisodeListItemResponse;
import com.streamflow.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Phase 1: Episode create and list under a season.
 * Routes: POST/GET /api/seasons/{seasonId}/episodes
 */
@RestController
@RequestMapping("/api/seasons")
public class SeasonController {

    private final ContentService contentService;

    public SeasonController(ContentService contentService) {
        this.contentService = contentService;
    }

    /**
     * Route 7: Create episode under a season.
     * POST /api/seasons/{seasonId}/episodes
     */
    @PostMapping("/{seasonId}/episodes")
    public ResponseEntity<EpisodeListItemResponse> createEpisode(
            @PathVariable UUID seasonId,
            @Valid @RequestBody CreateEpisodeRequest request) {
        EpisodeListItemResponse created = contentService.createEpisode(seasonId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Route 8: List episodes for a season (episodeNumber ASC).
     * GET /api/seasons/{seasonId}/episodes
     */
    @GetMapping("/{seasonId}/episodes")
    public ResponseEntity<List<EpisodeListItemResponse>> getEpisodes(@PathVariable UUID seasonId) {
        List<EpisodeListItemResponse> list = contentService.getEpisodesForSeason(seasonId);
        return ResponseEntity.ok(list);
    }
}
