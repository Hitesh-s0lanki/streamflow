# Streamflow Entity Types

Reference for all types defined in `com.streamflow.entity` and `com.streamflow.entity.enums`.

---

## Base type

### BaseEntity (abstract)

Mapped superclass for all entities. Provides common fields.

| Field       | Type     | Nullable | Description                          |
|------------|----------|----------|--------------------------------------|
| id         | UUID     | No       | Primary key (generated)              |
| createdAt  | Instant  | No       | Creation timestamp                   |
| updatedAt  | Instant  | No       | Last update timestamp                |
| status     | String   | Yes      | Optional status (max 32 chars)       |

---

## Enums

### ContentType

Type of content: Movie (single video) or Series (seasons/episodes).

| Value   |
|--------|
| MOVIE  |
| SERIES |

---

### IngestionStatus

Lifecycle state of an ingestion job (upload → processing → ready).

| Value             |
|-------------------|
| PENDING           |
| UPLOADING         |
| UPLOADED          |
| PROCESSING        |
| TRANSCODED        |
| SPRITES_GENERATED |
| READY             |
| FAILED            |

---

### LicenseStatus

Status of a playback license (issued, revoked, expired).

| Value   |
|--------|
| ACTIVE |
| REVOKED|
| EXPIRED|

---

### PlaybackEventType

Types of playback events for analytics (Kafka / event-driven).

| Value     |
|----------|
| PLAY     |
| PAUSE    |
| SEEK     |
| BUFFERING|
| COMPLETED|
| ABANDONED|
| ERROR    |

---

### PublishStatus

Visibility of content in the catalog (draft vs published).

| Value     |
|----------|
| DRAFT    |
| PUBLISHED|

---

## Entities

### Content

Root catalog entity: a Movie or Series visible in the UI (home, search, preview).

**Extends:** BaseEntity  
**Table:** `content`

| Field            | Type         | Nullable | Description                                                       |
|-----------------|--------------|----------|-------------------------------------------------------------------|
| title           | String       | No       | Max 512 chars                                                     |
| description     | String (TEXT)| Yes      |                                                                   |
| contentType     | ContentType  | No       | MOVIE or SERIES                                                   |
| releaseYear     | Integer      | Yes      |                                                                   |
| rating          | String       | Yes      | Max 16 chars                                                      |
| posterUrl       | String       | Yes      | Max 1024 chars                                                    |
| thumbnailUrl    | String       | Yes      | Max 1024 chars                                                    |
| publishStatus   | PublishStatus| No       | Default: DRAFT                                                    |
| durationSeconds | Integer      | Yes      | For MOVIE. For SERIES, derived from episodes                       |
| seasons         | List\<SeriesSeason\> | —  | One-to-many (ordered by seasonNumber)                    |
| videoAsset      | VideoAsset   | Yes      | One-to-one. For MOVIE: single asset. For SERIES: use Episode → VideoAsset |

---

### SeriesSeason

A season within a Series. Only used when `Content.contentType = SERIES`.

**Extends:** BaseEntity  
**Table:** `series_season`

| Field         | Type      | Nullable | Description                          |
|--------------|-----------|----------|--------------------------------------|
| content      | Content   | No       | Many-to-one                          |
| seasonNumber | Integer   | No       |                                      |
| title        | String    | Yes      | Max 512 chars                        |
| posterUrl    | String    | Yes      | Max 1024 chars                       |
| episodes     | List\<Episode\> | —   | One-to-many (ordered by episodeNumber) |

---

### Episode

A single episode of a series. Each episode has one playable VideoAsset.

**Extends:** BaseEntity  
**Table:** `episode`

| Field           | Type       | Nullable | Description     |
|----------------|------------|----------|-----------------|
| season         | SeriesSeason | No     | Many-to-one     |
| episodeNumber  | Integer    | No       |                 |
| title          | String     | No       | Max 512 chars   |
| description    | String (TEXT) | Yes   |                 |
| durationSeconds| Integer    | No       | Duration in seconds |
| thumbnailUrl   | String     | Yes      | Max 1024 chars   |
| videoAsset     | VideoAsset | No       | One-to-one       |

---

### VideoAsset

A single playable video unit. Movie → 1 VideoAsset; Episode → 1 VideoAsset.

**Extends:** BaseEntity  
**Table:** `video_asset`

| Field           | Type        | Nullable | Description                                      |
|----------------|-------------|----------|--------------------------------------------------|
| content        | Content     | Yes      | Many-to-one. For MOVIE: the content. For SERIES: the series. |
| episode        | Episode     | Yes      | One-to-one. For SERIES episode; null for movie.   |
| durationSeconds| Integer     | No       | Duration in seconds                              |
| manifestUrl    | String      | Yes      | Max 1024 chars                                   |
| drmEnabled     | Boolean     | No       | Default: false                                   |
| variants       | List\<VideoVariant\> | — | One-to-many (ABR renditions)             |
| spriteSheets   | List\<SpriteSheet\>  | — | One-to-many (preview thumbnails)         |
| ingestionJobs  | List\<IngestionJob\> | — | One-to-many                              |

---

### VideoVariant

One ABR rendition of a video (resolution, bitrate, codec, segment path).

**Extends:** BaseEntity  
**Table:** `video_variant`

| Field       | Type      | Nullable | Description   |
|------------|-----------|----------|---------------|
| videoAsset | VideoAsset| No       | Many-to-one   |
| resolution | String    | No       | Max 32 chars   |
| bitrateKbps| Integer   | Yes      |               |
| codec      | String    | Yes      | Max 64 chars   |
| segmentPath| String    | Yes      | Max 1024 chars |
| sortOrder  | Integer   | Yes      |               |

---

### SpriteSheet

One sprite image containing many preview frames for Netflix-style seek preview.

**Extends:** BaseEntity  
**Table:** `sprite_sheet`

| Field             | Type               | Nullable | Description   |
|------------------|--------------------|----------|---------------|
| videoAsset       | VideoAsset         | No       | Many-to-one   |
| spriteUrl        | String             | No       | Max 1024 chars|
| startTimeSeconds | Integer            | No       |               |
| endTimeSeconds   | Integer            | No       |               |
| columns          | Integer            | No       |               |
| rows             | Integer            | No       |               |
| thumbnailWidth   | Integer            | No       |               |
| thumbnailHeight  | Integer            | No       |               |
| intervalSeconds  | Integer            | Yes      |               |
| frameMetadata   | List\<SpriteFrameMetadata\> | — | One-to-many |

---

### SpriteFrameMetadata

Per-frame coordinates and time offset for a sprite sheet (advanced preview mapping).

**Extends:** BaseEntity  
**Table:** `sprite_frame_metadata`

| Field              | Type       | Nullable | Description |
|-------------------|------------|----------|-------------|
| spriteSheet       | SpriteSheet| No       | Many-to-one |
| frameIndex        | Integer    | No       |             |
| timeOffsetSeconds | Integer    | No       |             |
| xPosition         | Integer    | Yes      |             |
| yPosition         | Integer    | Yes      |             |
| width             | Integer    | Yes      |             |
| height            | Integer    | Yes      |             |

---

### IngestionJob

Tracks ingestion lifecycle for a video upload: raw S3 key, processing state, errors.

**Extends:** BaseEntity  
**Table:** `ingestion_job`

| Field       | Type       | Nullable | Description        |
|------------|------------|----------|--------------------|
| videoAsset | VideoAsset | No       | Many-to-one        |
| jobStatus  | IngestionStatus | No  | Default: PENDING    |
| rawS3Key   | String     | Yes      | Max 1024 chars      |
| errorMessage | String (TEXT) | Yes  |                    |
| processedAt| Instant    | Yes      |                    |

---

### PlaybackLicense

Temporary permission to decrypt/play a video. userId from Clerk.

**Extends:** BaseEntity  
**Table:** `playback_license`

| Field        | Type          | Nullable | Description   |
|-------------|---------------|----------|---------------|
| userId      | String        | No       | Clerk user id, max 256 chars |
| videoAsset  | VideoAsset    | No       | Many-to-one   |
| expiresAt   | Instant       | No       |               |
| licenseStatus | LicenseStatus| No     | Default: ACTIVE |
| deviceId    | String        | Yes      | Max 256 chars  |

---

### SignedPlaybackUrl

Short-lived signed URL record for manifests/segments. Optional, for auditing/debugging.

**Extends:** BaseEntity  
**Table:** `signed_playback_url`

| Field      | Type       | Nullable | Description   |
|-----------|------------|----------|---------------|
| videoAsset| VideoAsset | No       | Many-to-one   |
| signedUrl | String (TEXT) | No    |               |
| expiresAt | Instant    | No       |               |
| urlType   | String     | Yes      | Max 32 chars  |

---

### WatchProgress

Resume position per user (Clerk userId) and video asset.

**Extends:** BaseEntity  
**Table:** `watch_progress`

| Field           | Type       | Nullable | Description        |
|----------------|------------|----------|--------------------|
| userId         | String     | No       | Max 256 chars      |
| videoAsset     | VideoAsset | No       | Many-to-one        |
| lastWatchedSecond | Integer | No       | Default: 0         |
| completed      | Boolean    | No       | Default: false     |
| lastWatchedAt  | Instant    | Yes      |                    |

---

### PlaybackEventLog

Sampled playback events for debugging; full stream goes to Kafka.

**Extends:** BaseEntity  
**Table:** `playback_event_log`

| Field             | Type             | Nullable | Description   |
|------------------|------------------|----------|---------------|
| userId           | String           | Yes      | Max 256 chars |
| videoAsset       | VideoAsset       | Yes      | Many-to-one   |
| eventType        | PlaybackEventType| No       |               |
| currentTimeSeconds | Integer        | Yes      |               |
| payload          | String (TEXT)    | Yes      |               |

---

### PlaybackAnalytics

Aggregated playback metrics derived from Kafka consumers (optional).

**Extends:** BaseEntity  
**Table:** `playback_analytics`

| Field              | Type       | Nullable | Description   |
|-------------------|------------|----------|---------------|
| videoAsset        | VideoAsset | No       | Many-to-one   |
| periodStart       | Instant    | No       |               |
| periodEnd         | Instant    | No       |               |
| totalPlays        | Long       | Yes      |               |
| uniqueViewers     | Long       | Yes      |               |
| avgWatchTimeSeconds | Integer  | Yes      |               |
| completionRate    | BigDecimal | Yes      | precision 5, scale 4 |
| bufferingRate     | BigDecimal | Yes      | precision 5, scale 4 |

---

## Relationship summary

```
Content (1) ──┬── (N) SeriesSeason ── (N) Episode ── (1) VideoAsset
              └── (0..1) VideoAsset  (for MOVIE)

VideoAsset (1) ── (N) VideoVariant
            ── (N) SpriteSheet ── (N) SpriteFrameMetadata
            ── (N) IngestionJob
            ── (N) PlaybackLicense
            ── (N) SignedPlaybackUrl
            ── (N) WatchProgress
            ── (N) PlaybackEventLog
            ── (N) PlaybackAnalytics
```
