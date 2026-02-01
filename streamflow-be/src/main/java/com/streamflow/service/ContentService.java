package com.streamflow.service;

import com.streamflow.entity.Content;
import com.streamflow.entity.enums.ContentType;
import com.streamflow.entity.enums.PublishStatus;
import com.streamflow.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;

    @Transactional(readOnly = true)
    public Optional<Content> findById(UUID id) {
        return contentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Content> findPublished(Pageable pageable) {
        return contentRepository.findByPublishStatus(PublishStatus.PUBLISHED, pageable);
    }

    @Transactional(readOnly = true)
    public List<Content> findByTypeAndPublished(ContentType contentType, Pageable pageable) {
        return contentRepository.findByContentTypeAndPublishStatus(contentType, PublishStatus.PUBLISHED, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Content> searchPublished(String query, Pageable pageable) {
        return contentRepository.searchPublished(query, PublishStatus.PUBLISHED, pageable);
    }

    @Transactional
    public Content save(Content content) {
        return contentRepository.save(content);
    }

    @Transactional
    public Content update(UUID id, Content updates) {
        Content existing = contentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + id));
        if (updates.getTitle() != null)
            existing.setTitle(updates.getTitle());
        if (updates.getDescription() != null)
            existing.setDescription(updates.getDescription());
        if (updates.getContentType() != null)
            existing.setContentType(updates.getContentType());
        if (updates.getReleaseYear() != null)
            existing.setReleaseYear(updates.getReleaseYear());
        if (updates.getRating() != null)
            existing.setRating(updates.getRating());
        if (updates.getPosterUrl() != null)
            existing.setPosterUrl(updates.getPosterUrl());
        if (updates.getThumbnailUrl() != null)
            existing.setThumbnailUrl(updates.getThumbnailUrl());
        if (updates.getPublishStatus() != null)
            existing.setPublishStatus(updates.getPublishStatus());
        if (updates.getDurationSeconds() != null)
            existing.setDurationSeconds(updates.getDurationSeconds());
        return contentRepository.save(existing);
    }

    @Transactional
    public void deleteById(UUID id) {
        contentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsByTitleAndType(String title, ContentType contentType) {
        return contentRepository.existsByTitleAndContentType(title, contentType);
    }
}
