package com.streamflow.service;

import com.streamflow.entity.PlaybackEventLog;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.enums.PlaybackEventType;
import com.streamflow.repository.PlaybackEventLogRepository;
import com.streamflow.repository.VideoAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaybackEventLogService {

    private final PlaybackEventLogRepository playbackEventLogRepository;
    private final VideoAssetRepository videoAssetRepository;

    @Transactional(readOnly = true)
    public Optional<PlaybackEventLog> findById(UUID id) {
        return playbackEventLogRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<PlaybackEventLog> findByVideoAssetId(UUID videoAssetId, Pageable pageable) {
        return playbackEventLogRepository.findByVideoAssetId(videoAssetId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PlaybackEventLog> findByUserId(String userId, Pageable pageable) {
        return playbackEventLogRepository.findByUserId(userId, pageable);
    }

    @Transactional
    public PlaybackEventLog log(String userId, UUID videoAssetId, PlaybackEventType eventType,
            Integer currentTimeSeconds, String payload) {
        PlaybackEventLog log = new PlaybackEventLog();
        log.setUserId(userId);
        if (videoAssetId != null) {
            videoAssetRepository.findById(videoAssetId).ifPresent(log::setVideoAsset);
        }
        log.setEventType(eventType);
        log.setCurrentTimeSeconds(currentTimeSeconds);
        log.setPayload(payload);
        return playbackEventLogRepository.save(log);
    }

    @Transactional
    public void deleteById(UUID id) {
        playbackEventLogRepository.deleteById(id);
    }
}
