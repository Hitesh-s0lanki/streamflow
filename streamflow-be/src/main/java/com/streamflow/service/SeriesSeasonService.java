package com.streamflow.service;

import com.streamflow.entity.Content;
import com.streamflow.entity.SeriesSeason;
import com.streamflow.repository.ContentRepository;
import com.streamflow.repository.SeriesSeasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeriesSeasonService {

    private final SeriesSeasonRepository seriesSeasonRepository;
    private final ContentRepository contentRepository;

    @Transactional(readOnly = true)
    public Optional<SeriesSeason> findById(UUID id) {
        return seriesSeasonRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<SeriesSeason> findByContentId(UUID contentId) {
        return seriesSeasonRepository.findByContentIdOrderBySeasonNumberAsc(contentId);
    }

    @Transactional(readOnly = true)
    public List<SeriesSeason> findByContentIdWithEpisodes(UUID contentId) {
        return seriesSeasonRepository.findByContentIdWithEpisodes(contentId);
    }

    @Transactional(readOnly = true)
    public Optional<SeriesSeason> findByContentIdAndSeasonNumber(UUID contentId, Integer seasonNumber) {
        return seriesSeasonRepository.findByContentIdAndSeasonNumber(contentId, seasonNumber);
    }

    @Transactional
    public SeriesSeason create(UUID contentId, Integer seasonNumber, String title, String posterUrl) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));
        if (seriesSeasonRepository.existsByContentIdAndSeasonNumber(contentId, seasonNumber)) {
            throw new IllegalArgumentException("Season " + seasonNumber + " already exists for content " + contentId);
        }
        SeriesSeason season = new SeriesSeason();
        season.setContent(content);
        season.setSeasonNumber(seasonNumber);
        season.setTitle(title != null ? title : "Season " + seasonNumber);
        season.setPosterUrl(posterUrl);
        return seriesSeasonRepository.save(season);
    }

    @Transactional
    public SeriesSeason save(SeriesSeason season) {
        return seriesSeasonRepository.save(season);
    }

    @Transactional
    public void deleteById(UUID id) {
        seriesSeasonRepository.deleteById(id);
    }
}
