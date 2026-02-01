# Streamflow Backend

API + Media Worker for the Streamflow OTT backend.

## Layout

- **Root (streamflow)** – REST API: admin upload, presigned URLs, ingestion jobs, Kafka producer.
- **media-worker** – Kafka consumer + FFmpeg pipeline: transcode to HLS, sprites, S3 upload, DB updates.

## Build

```bash
mvn package -DskipTests
```

## Run API

From repo root:

```bash
mvn spring-boot:run
```

Or:

```bash
java -jar target/streamflow-0.0.1-SNAPSHOT-exec.jar
```

## Run Media Worker

```bash
mvn -pl media-worker spring-boot:run
```

See [media-worker/README.md](media-worker/README.md) for worker config and pipeline details.

## Requirements

- Java 21
- PostgreSQL (same DB for API and worker)
- Kafka (for ingestion events)
- S3 (raw + packaged buckets)
- FFmpeg (worker only)
