package com.streamflow.service;

import com.streamflow.dto.*;
import com.streamflow.entity.Content;
import com.streamflow.entity.Episode;
import com.streamflow.entity.SeriesSeason;
import com.streamflow.entity.enums.ContentType;
import com.streamflow.entity.enums.PublishStatus;
import com.streamflow.exception.BadRequestException;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.repository.ContentRepository;
import com.streamflow.repository.EpisodeRepository;
import com.streamflow.repository.SeriesSeasonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Phase 1 catalog: Content, SeriesSeason, Episode create/publish and read.
 */
@Service
public class ContentService {

    private final ContentRepository contentRepository;
    private final SeriesSeasonRepository seriesSeasonRepository;
    private final EpisodeRepository episodeRepository;

    public ContentService(ContentRepository contentRepository,
            SeriesSeasonRepository seriesSeasonRepository,
            EpisodeRepository episodeRepository) {
        this.contentRepository = contentRepository;
        this.seriesSeasonRepository = seriesSeasonRepository;
        this.episodeRepository = episodeRepository;
    }

    @Transactional
    public ContentDetailResponse createContent(CreateContentRequest request) {
        Content content = new Content();
        content.setTitle(request.getTitle().trim());
        content.setContentType(request.getContentType());
        content.setPublishStatus(PublishStatus.DRAFT);
        content.setDescription(trimOrNull(request.getDescription()));
        content.setReleaseYear(request.getReleaseYear());
        content.setRating(trimOrNull(request.getRating()));
        content.setPosterUrl(trimOrNull(request.getPosterUrl()));
        content.setThumbnailUrl(trimOrNull(request.getThumbnailUrl()));
        content.setDurationSeconds(null);
        content = contentRepository.save(content);
        return toDetailResponse(content, true);
    }

    @Transactional
    public ContentDetailResponse publishContent(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));
        if (content.getPublishStatus() != PublishStatus.DRAFT) {
            throw new BadRequestException(
                    "Content can only be published from DRAFT; current status: " + content.getPublishStatus());
        }
        content.setPublishStatus(PublishStatus.PUBLISHED);
        content = contentRepository.save(content);
        return toDetailResponse(content, true);
    }

    /** Admin: list all content (including DRAFT) with filters and paging. Order: createdAt DESC. */
    @Transactional(readOnly = true)
    public PagedResponse<AdminContentListItemResponse> adminListContent(PublishStatus publishStatus,
            ContentType contentType, String title, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Content> p = contentRepository.findAdminList(publishStatus, contentType, trimOrNull(title), pageable);
        List<AdminContentListItemResponse> items = p.getContent().stream()
                .map(this::toAdminListItem)
                .toList();
        return PagedResponse.<AdminContentListItemResponse>builder()
                .content(items)
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .first(p.isFirst())
                .last(p.isLast())
                .build();
    }

    /** Admin: update content metadata (no contentType change). */
    @Transactional
    public ContentDetailResponse updateContentMetadata(UUID contentId, UpdateContentMetadataRequest request) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            content.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            content.setDescription(trimOrNull(request.getDescription()));
        }
        if (request.getReleaseYear() != null) {
            content.setReleaseYear(request.getReleaseYear());
        }
        if (request.getRating() != null) {
            content.setRating(trimOrNull(request.getRating()));
        }
        if (request.getPosterUrl() != null) {
            content.setPosterUrl(trimOrNull(request.getPosterUrl()));
        }
        if (request.getThumbnailUrl() != null) {
            content.setThumbnailUrl(trimOrNull(request.getThumbnailUrl()));
        }
        content = contentRepository.save(content);
        return toDetailResponse(content, true);
    }

    /** Admin: unpublish content (PUBLISHED → DRAFT). Instantly removed from public catalog. */
    @Transactional
    public ContentDetailResponse unpublishContent(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));
        if (content.getPublishStatus() != PublishStatus.PUBLISHED) {
            throw new BadRequestException(
                    "Content can only be unpublished from PUBLISHED; current status: " + content.getPublishStatus());
        }
        content.setPublishStatus(PublishStatus.DRAFT);
        content = contentRepository.save(content);
        return toDetailResponse(content, true);
    }

    private AdminContentListItemResponse toAdminListItem(Content c) {
        return AdminContentListItemResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .contentType(c.getContentType())
                .publishStatus(c.getPublishStatus())
                .posterUrl(c.getPosterUrl())
                .thumbnailUrl(c.getThumbnailUrl())
                .releaseYear(c.getReleaseYear())
                .durationSeconds(c.getDurationSeconds())
                .createdAt(c.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ContentCatalogItemResponse> getCatalogListing() {
        List<Content> list = contentRepository.findByPublishStatusOrderByCreatedAtDesc(PublishStatus.PUBLISHED);
        List<ContentCatalogItemResponse> result = new ArrayList<>(list.size());
        for (Content c : list) {
            result.add(ContentCatalogItemResponse.builder()
                    .id(c.getId())
                    .title(c.getTitle())
                    .contentType(c.getContentType())
                    .posterUrl(c.getPosterUrl())
                    .thumbnailUrl(c.getThumbnailUrl())
                    .releaseYear(c.getReleaseYear())
                    .durationSeconds(c.getDurationSeconds())
                    .build());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ContentDetailResponse getContentDetail(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));
        return toDetailResponse(content, true);
    }

    @Transactional
    public SeasonSummaryResponse createSeason(UUID contentId, CreateSeasonRequest request) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));
        if (content.getContentType() != ContentType.SERIES) {
            throw new BadRequestException("Seasons can only be added to content with contentType SERIES");
        }
        if (seriesSeasonRepository.existsByContentIdAndSeasonNumber(contentId, request.getSeasonNumber())) {
            throw new BadRequestException(
                    "Season number " + request.getSeasonNumber() + " already exists for this content");
        }
        SeriesSeason season = new SeriesSeason();
        season.setContent(content);
        season.setSeasonNumber(request.getSeasonNumber());
        season.setTitle(trimOrNull(request.getTitle()));
        season.setPosterUrl(trimOrNull(request.getPosterUrl()));
        season = seriesSeasonRepository.save(season);
        return toSeasonSummary(season);
    }

    @Transactional(readOnly = true)
    public List<SeasonSummaryResponse> getSeasonsForContent(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));
        if (content.getContentType() != ContentType.SERIES) {
            throw new BadRequestException("Seasons are only available for content with contentType SERIES");
        }
        List<SeriesSeason> seasons = seriesSeasonRepository.findByContentIdOrderBySeasonNumberAsc(contentId);
        List<SeasonSummaryResponse> result = new ArrayList<>(seasons.size());
        for (SeriesSeason s : seasons) {
            result.add(toSeasonSummary(s));
        }
        return result;
    }

    @Transactional
    public EpisodeListItemResponse createEpisode(UUID seasonId, CreateEpisodeRequest request) {
        SeriesSeason season = seriesSeasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Season", seasonId));
        if (episodeRepository.existsBySeasonIdAndEpisodeNumber(seasonId, request.getEpisodeNumber())) {
            throw new BadRequestException(
                    "Episode number " + request.getEpisodeNumber() + " already exists for this season");
        }
        Episode episode = new Episode();
        episode.setSeason(season);
        episode.setEpisodeNumber(request.getEpisodeNumber());
        episode.setTitle(request.getTitle().trim());
        episode.setDurationSeconds(request.getDurationSeconds());
        episode.setDescription(trimOrNull(request.getDescription()));
        episode.setThumbnailUrl(trimOrNull(request.getThumbnailUrl()));
        episode.setVideoAsset(null);
        episode = episodeRepository.save(episode);
        return toEpisodeListItem(episode);
    }

    @Transactional(readOnly = true)
    public List<EpisodeListItemResponse> getEpisodesForSeason(UUID seasonId) {
        if (!seriesSeasonRepository.existsById(seasonId)) {
            throw new ResourceNotFoundException("Season", seasonId);
        }
        List<Episode> episodes = episodeRepository.findBySeasonIdOrderByEpisodeNumberAsc(seasonId);
        List<EpisodeListItemResponse> result = new ArrayList<>(episodes.size());
        for (Episode e : episodes) {
            result.add(toEpisodeListItem(e));
        }
        return result;
    }

    private ContentDetailResponse toDetailResponse(Content content, boolean loadSeasons) {
        List<SeasonSummaryResponse> seasons = List.of();
        if (content.getContentType() == ContentType.SERIES && loadSeasons) {
            List<SeriesSeason> list = seriesSeasonRepository.findByContentIdOrderBySeasonNumberAsc(content.getId());
            seasons = list.stream().map(this::toSeasonSummary).toList();
        }
        return ContentDetailResponse.builder()
                .id(content.getId())
                .title(content.getTitle())
                .description(content.getDescription())
                .contentType(content.getContentType())
                .releaseYear(content.getReleaseYear())
                .rating(content.getRating())
                .posterUrl(content.getPosterUrl())
                .thumbnailUrl(content.getThumbnailUrl())
                .publishStatus(content.getPublishStatus())
                .durationSeconds(content.getDurationSeconds())
                .createdAt(content.getCreatedAt())
                .updatedAt(content.getUpdatedAt())
                .seasons(seasons)
                .build();
    }

    private SeasonSummaryResponse toSeasonSummary(SeriesSeason s) {
        return SeasonSummaryResponse.builder()
                .id(s.getId())
                .seasonNumber(s.getSeasonNumber())
                .title(s.getTitle())
                .posterUrl(s.getPosterUrl())
                .build();
    }

    private EpisodeListItemResponse toEpisodeListItem(Episode e) {
        return EpisodeListItemResponse.builder()
                .id(e.getId())
                .episodeNumber(e.getEpisodeNumber())
                .title(e.getTitle())
                .durationSeconds(e.getDurationSeconds())
                .thumbnailUrl(e.getThumbnailUrl())
                .build();
    }

    private static String trimOrNull(String s) {
        if (s == null || s.isBlank())
            return null;
        return s.trim();
    }
}
