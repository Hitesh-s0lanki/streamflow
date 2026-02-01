package com.streamflow.repository;

import com.streamflow.entity.SignedPlaybackUrl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface SignedPlaybackUrlRepository extends JpaRepository<SignedPlaybackUrl, UUID> {

        @Modifying
        @Query("DELETE FROM SignedPlaybackUrl s WHERE s.expiresAt < :before")
        int deleteExpiredBefore(@Param("before") Instant before);

        /**
         * Admin audit list: filters videoAssetId, urlType, createdAt range. Order:
         * createdAt DESC. Metadata only.
         */
        @Query("SELECT s FROM SignedPlaybackUrl s WHERE (:videoAssetId IS NULL OR s.videoAsset.id = :videoAssetId)"
                        + " AND (:urlType IS NULL OR :urlType = '' OR s.urlType = :urlType)"
                        + " AND (:from IS NULL OR s.createdAt >= :from) AND (:to IS NULL OR s.createdAt <= :to)"
                        + " ORDER BY s.createdAt DESC")
        Page<SignedPlaybackUrl> findAdminList(@Param("videoAssetId") UUID videoAssetId,
                        @Param("urlType") String urlType,
                        @Param("from") Instant from,
                        @Param("to") Instant to,
                        Pageable pageable);
}
