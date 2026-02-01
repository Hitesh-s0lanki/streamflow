package com.streamflow.repository;

import com.streamflow.entity.SignedPlaybackUrl;
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
}
