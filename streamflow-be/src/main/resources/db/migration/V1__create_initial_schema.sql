-- Streamflow initial schema
-- Base fields (id UUID, created_at, updated_at, status) on each table

CREATE TABLE IF NOT EXISTS content (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(32),
    title VARCHAR(512) NOT NULL,
    description TEXT,
    content_type VARCHAR(32) NOT NULL,
    release_year INTEGER,
    rating VARCHAR(16),
    poster_url VARCHAR(1024),
    thumbnail_url VARCHAR(1024),
    publish_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    duration_seconds INTEGER
);
CREATE INDEX IF NOT EXISTS idx_content_content_type ON content(content_type);
CREATE INDEX IF NOT EXISTS idx_content_publish_status ON content(publish_status);
CREATE INDEX IF NOT EXISTS idx_content_release_year ON content(release_year);

CREATE TABLE IF NOT EXISTS video_asset (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(32),
    content_id UUID REFERENCES content(id),
    episode_id UUID UNIQUE,
    duration_seconds INTEGER NOT NULL,
    manifest_url VARCHAR(1024),
    drm_enabled BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_video_asset_content_id ON video_asset(content_id);
CREATE INDEX IF NOT EXISTS idx_video_asset_episode_id ON video_asset(episode_id);

CREATE TABLE IF NOT EXISTS series_season (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(32),
    content_id UUID NOT NULL REFERENCES content(id),
    season_number INTEGER NOT NULL,
    title VARCHAR(512),
    poster_url VARCHAR(1024),
    UNIQUE(content_id, season_number)
);
CREATE INDEX IF NOT EXISTS idx_series_season_content_id ON series_season(content_id);
CREATE INDEX IF NOT EXISTS idx_series_season_number ON series_season(content_id, season_number);

CREATE TABLE IF NOT EXISTS episode (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(32),
    season_id UUID NOT NULL REFERENCES series_season(id),
    episode_number INTEGER NOT NULL,
    title VARCHAR(512) NOT NULL,
    description TEXT,
    duration_seconds INTEGER NOT NULL,
    thumbnail_url VARCHAR(1024),
    video_asset_id UUID NOT NULL UNIQUE REFERENCES video_asset(id),
    UNIQUE(season_id, episode_number)
);
CREATE INDEX IF NOT EXISTS idx_episode_season_id ON episode(season_id);
CREATE INDEX IF NOT EXISTS idx_episode_video_asset_id ON episode(video_asset_id);
CREATE INDEX IF NOT EXISTS idx_episode_season_number ON episode(season_id, episode_number);

CREATE TABLE IF NOT EXISTS video_variant (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(32),
    video_asset_id UUID NOT NULL REFERENCES video_asset(id),
    resolution VARCHAR(32) NOT NULL,
    bitrate_kbps INTEGER,
    codec VARCHAR(64),
    segment_path VARCHAR(1024),
    sort_order INTEGER
);
CREATE INDEX IF NOT EXISTS idx_video_variant_video_asset_id ON video_variant(video_asset_id);
CREATE INDEX IF NOT EXISTS idx_video_variant_resolution ON video_variant(video_asset_id, resolution);

CREATE TABLE IF NOT EXISTS ingestion_job (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(32),
    video_asset_id UUID NOT NULL REFERENCES video_asset(id),
    job_status VARCHAR(32) NOT NULL,
    raw_s3_key VARCHAR(1024),
    error_message TEXT,
    processed_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ingestion_job_video_asset_id ON ingestion_job(video_asset_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_job_status ON ingestion_job(job_status);
CREATE INDEX IF NOT EXISTS idx_ingestion_job_created_at ON ingestion_job(created_at);

CREATE TABLE IF NOT EXISTS sprite_sheet (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(32),
    video_asset_id UUID NOT NULL REFERENCES video_asset(id),
    sprite_url VARCHAR(1024) NOT NULL,
    start_time_seconds INTEGER NOT NULL,
    end_time_seconds INTEGER NOT NULL,
    "columns" INTEGER NOT NULL,
    "rows" INTEGER NOT NULL,
    thumbnail_width INTEGER NOT NULL,
    thumbnail_height INTEGER NOT NULL,
    interval_seconds INTEGER
);
CREATE INDEX IF NOT EXISTS idx_sprite_sheet_video_asset_id ON sprite_sheet(video_asset_id);
CREATE INDEX IF NOT EXISTS idx_sprite_sheet_time_range ON sprite_sheet(video_asset_id, start_time_seconds, end_time_seconds);

CREATE TABLE IF NOT EXISTS sprite_frame_metadata (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(32),
    sprite_sheet_id UUID NOT NULL REFERENCES sprite_sheet(id),
    frame_index INTEGER NOT NULL,
    time_offset_seconds INTEGER NOT NULL,
    x_position INTEGER,
    y_position INTEGER,
    width INTEGER,
    height INTEGER
);
CREATE INDEX IF NOT EXISTS idx_sprite_frame_sprite_sheet_id ON sprite_frame_metadata(sprite_sheet_id);
CREATE INDEX IF NOT EXISTS idx_sprite_frame_time ON sprite_frame_metadata(sprite_sheet_id, time_offset_seconds);

CREATE TABLE IF NOT EXISTS playback_license (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(32),
    user_id VARCHAR(256) NOT NULL,
    video_asset_id UUID NOT NULL REFERENCES video_asset(id),
    expires_at TIMESTAMP NOT NULL,
    license_status VARCHAR(32) NOT NULL,
    device_id VARCHAR(256)
);
CREATE INDEX IF NOT EXISTS idx_playback_license_video_asset_id ON playback_license(video_asset_id);
CREATE INDEX IF NOT EXISTS idx_playback_license_user_id ON playback_license(user_id);
CREATE INDEX IF NOT EXISTS idx_playback_license_user_asset ON playback_license(user_id, video_asset_id);
CREATE INDEX IF NOT EXISTS idx_playback_license_expires_at ON playback_license(expires_at);
CREATE INDEX IF NOT EXISTS idx_playback_license_status ON playback_license(license_status);

CREATE TABLE IF NOT EXISTS signed_playback_url (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(32),
    video_asset_id UUID NOT NULL REFERENCES video_asset(id),
    signed_url TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    url_type VARCHAR(32)
);
CREATE INDEX IF NOT EXISTS idx_signed_playback_url_video_asset_id ON signed_playback_url(video_asset_id);
CREATE INDEX IF NOT EXISTS idx_signed_playback_url_expires_at ON signed_playback_url(expires_at);

CREATE TABLE IF NOT EXISTS watch_progress (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(32),
    user_id VARCHAR(256) NOT NULL,
    video_asset_id UUID NOT NULL REFERENCES video_asset(id),
    last_watched_second INTEGER NOT NULL DEFAULT 0,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    last_watched_at TIMESTAMP,
    UNIQUE(user_id, video_asset_id)
);
CREATE INDEX IF NOT EXISTS idx_watch_progress_user_id ON watch_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_watch_progress_video_asset_id ON watch_progress(video_asset_id);
CREATE INDEX IF NOT EXISTS idx_watch_progress_user_asset ON watch_progress(user_id, video_asset_id);

CREATE TABLE IF NOT EXISTS playback_event_log (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(32),
    user_id VARCHAR(256),
    video_asset_id UUID REFERENCES video_asset(id),
    event_type VARCHAR(32) NOT NULL,
    current_time_seconds INTEGER,
    payload TEXT
);
CREATE INDEX IF NOT EXISTS idx_playback_event_video_asset_id ON playback_event_log(video_asset_id);
CREATE INDEX IF NOT EXISTS idx_playback_event_user_id ON playback_event_log(user_id);
CREATE INDEX IF NOT EXISTS idx_playback_event_created_at ON playback_event_log(created_at);
CREATE INDEX IF NOT EXISTS idx_playback_event_type ON playback_event_log(event_type);

CREATE TABLE IF NOT EXISTS playback_analytics (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(32),
    video_asset_id UUID NOT NULL REFERENCES video_asset(id),
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    total_plays BIGINT,
    unique_viewers BIGINT,
    avg_watch_time_seconds INTEGER,
    completion_rate DECIMAL(5,4),
    buffering_rate DECIMAL(5,4)
);
CREATE INDEX IF NOT EXISTS idx_playback_analytics_video_asset_id ON playback_analytics(video_asset_id);
CREATE INDEX IF NOT EXISTS idx_playback_analytics_period ON playback_analytics(video_asset_id, period_start);

-- video_asset.episode_id -> episode (circular ref; add after episode exists)
ALTER TABLE video_asset ADD CONSTRAINT fk_video_asset_episode FOREIGN KEY (episode_id) REFERENCES episode(id);
