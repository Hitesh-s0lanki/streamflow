package com.streamflow.repository;

import com.streamflow.entity.Content;
import com.streamflow.entity.enums.ContentType;
import com.streamflow.entity.enums.PublishStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ContentRepository extends JpaRepository<Content, UUID> {

    List<Content> findByPublishStatus(PublishStatus status, Pageable pageable);

    List<Content> findByContentTypeAndPublishStatus(ContentType contentType, PublishStatus status, Pageable pageable);

    @Query("SELECT c FROM Content c WHERE c.publishStatus = :status AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Content> searchPublished(@Param("q") String query, @Param("status") PublishStatus status, Pageable pageable);

    boolean existsByTitleAndContentType(String title, ContentType contentType);
}
