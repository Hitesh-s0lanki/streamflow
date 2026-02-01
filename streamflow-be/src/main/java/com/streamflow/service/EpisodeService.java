package com.streamflow.service;

import com.streamflow.entity.Episode;
import com.streamflow.entity.SeriesSeason;
import com.streamflow.entity.VideoAsset;
import com.streamflow.repository.EpisodeRepository;
import com.streamflow.repository.SeriesSeasonRepository;
import com.streamflow.repository.VideoAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final EpisodeRepository episodeRepository;
    private final SeriesSeasonRepository seriesSeasonRepository;
    private final VideoAssetRepository videoAssetRepository;

    @Transactional(readOnly = true)
    public Optional<Episode> findById(UUID id) {
        return episodeRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Episode> findByVideoAssetId(UUID videoAssetId) {
        return episodeRepository.findByVideoAssetId(videoAssetId);
    }

    @Transactional(readOnly = true)
    public List<Episode> findBySeasonId(UUID seasonId) {
        return episodeRepository.findBySeasonIdOrderByEpisodeNumberAsc(seasonId);
    }

    @Transactional(readOnly = true)
    public List<Episode> findBySeasonIdWithVideoAsset(UUID seasonId) {
        return episodeRepository.findBySeasonIdWithVideoAsset(seasonId);
    }

    @Transactional(readOnly = true)
    public Optional<Episode> findBySeasonIdAndEpisodeNumber(UUID seasonId, Integer episodeNumber) {
        return episodeRepository.findBySeasonIdAndEpisodeNumber(seasonId, episodeNumber);
    }

    @Transactional
    public Episode create(UUID seasonId, int episodeNumber, String title, String description, int durationSeconds,
            String thumbnailUrl) {
        SeriesSeason season = seriesSeasonRepository.findById(seasonId)
                .orElseThrow(() -> new IllegalArgumentException("Season not found: " + seasonId));
        if (episodeRepository.existsBySeasonIdAndEpisodeNumber(seasonId, episodeNumber)) {
            throw new IllegalArgumentException("Episode " + episodeNumber + " already exists in season " + seasonId);
        }
        VideoAsset asset = new VideoAsset();
        asset.setContent(season.getContent());
        asset.setDurationSeconds(durationSeconds);
        asset.setDrmEnabled(false);
        asset = videoAssetRepository.save(asset);

        Episode episode = new Episode();
        episode.setSeason(season);
        episode.setEpisodeNumber(episodeNumber);
        episode.setTitle(title);
        episode.setDescription(description);
        episode.setDurationSeconds(durationSeconds);
        episode.setThumbnailUrl(thumbnailUrl);
        episode.setVideoAsset(asset);
        asset.setEpisode(episode);
        return episodeRepository.save(episode);
    }

    @Transactional
    public Episode save(Episode episode) {
        return episodeRepository.save(episode);
    }

    @Transactional
    public void deleteById(UUID id) {
        episodeRepository.deleteById(id);
    }
}
