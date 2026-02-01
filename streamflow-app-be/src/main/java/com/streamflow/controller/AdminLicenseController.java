package com.streamflow.controller;

import com.streamflow.dto.PagedResponse;
import com.streamflow.dto.PlaybackLicenseResponse;
import com.streamflow.entity.enums.LicenseStatus;
import com.streamflow.service.PlaybackService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin: list licenses, revoke license. All under /api/admin/licenses.
 */
@RestController
@RequestMapping("/api/admin/licenses")
public class AdminLicenseController {

    private final PlaybackService playbackService;

    public AdminLicenseController(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    /**
     * List licenses with filters. Order: createdAt DESC. Paginated.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<PlaybackLicenseResponse>> listLicenses(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) UUID videoAssetId,
            @RequestParam(required = false) LicenseStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant expiresFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant expiresTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<PlaybackLicenseResponse> result = playbackService.adminListLicenses(
                userId, videoAssetId, status, expiresFrom, expiresTo, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Revoke a playback license. licenseStatus becomes REVOKED; all future signed URL requests must fail.
     */
    @PostMapping("/{licenseId}/revoke")
    public ResponseEntity<PlaybackLicenseResponse> revokeLicense(@PathVariable UUID licenseId) {
        PlaybackLicenseResponse response = playbackService.revokeLicense(licenseId);
        return ResponseEntity.ok(response);
    }
}
