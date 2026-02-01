# Streamflow Media Worker

Separate service that consumes Kafka ingestion events, downloads raw video from S3, transcodes with FFmpeg (multi-resolution HLS), generates sprite sheets, uploads packaged assets and sprites to S3, and updates the shared DB.

## Prerequisites

- **FFmpeg** on PATH (with libx264, aac, HLS support)
- **Java 21**
- Same **PostgreSQL** and **Kafka** as the API
- **S3**: raw bucket (source), packaged bucket (output for HLS + sprites)

## Build

From repo root:

```bash
mvn -q package -DskipTests
```

Worker JAR: `media-worker/target/streamflow-media-worker-0.0.1-SNAPSHOT.jar` (build from repo root: `mvn package -DskipTests`).

## Run

```bash
cd media-worker && java -jar target/streamflow-media-worker-0.0.1-SNAPSHOT.jar
```

Or from root:

```bash
mvn -pl media-worker spring-boot:run
```

## Configuration

Set via env or `application.properties`:

| Property                             | Description                                             |
| ------------------------------------ | ------------------------------------------------------- |
| `spring.datasource.url`              | Same DB as API                                          |
| `spring.kafka.bootstrap-servers`     | Kafka brokers                                           |
| `app.aws.raw-bucket`                 | S3 bucket for raw uploads                               |
| `app.aws.packaged-bucket`            | S3 bucket for HLS + sprites                             |
| `app.worker.ingestion-topic`         | Topic to consume (default: `streamflow.ingestion.jobs`) |
| `app.worker.temp-dir`                | Temp dir for download/FFmpeg (default: OS temp)         |
| `app.worker.sprite-interval-seconds` | Frame interval for sprites (default: 5)                 |
| `app.worker.ffmpeg-timeout-seconds`  | FFmpeg timeout (default: 3600)                          |

## Pipeline

1. Consume `streamflow.ingestion.jobs` (payload: `jobId`, `videoAssetId`, `rawS3Key`, `contentType`, `drmEnabled`).
2. Set job status → **PROCESSING**.
3. Download raw from S3 to temp.
4. Get duration (ffprobe).
5. Transcode to HLS (240p, 480p, 720p, 1080p) → `/tmp/{videoAssetId}/packaged/`.
6. Set job status → **TRANSCODED**.
7. Upload packaged dir to `s3://packaged-bucket/packaged/{videoAssetId}/`.
8. Update `VideoAsset.manifestUrl`, create `VideoVariant` rows.
9. Generate sprite sheets (frame every 5s, 160×90, 5×5 grid).
10. Set job status → **SPRITES_GENERATED**.
11. Upload sprites to `s3://packaged-bucket/sprites/{videoAssetId}/`.
12. Persist `SpriteSheet` and `SpriteFrameMetadata`.
13. Set job status → **READY**, set `processedAt`.
14. Optionally publish `media.ingestion.completed` (and transcode/sprites topics) when configured.

On failure: job status → **FAILED**, `errorMessage` set; temp dir cleaned up.

## API run

- From repo root: `mvn spring-boot:run` or `java -jar target/streamflow-0.0.1-SNAPSHOT-exec.jar`
