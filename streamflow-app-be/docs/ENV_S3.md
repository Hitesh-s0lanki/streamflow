# Environment variables for AWS S3 (images / videos upload)

The S3 upload feature is **disabled** by default. Enable it and set the bucket (and optionally credentials) via environment variables or `application.properties`.

## Required (when S3 is enabled)

| Env variable | Property | Description |
|--------------|----------|-------------|
| `STREAMFLOW_AWS_S3_ENABLED` | `streamflow.aws.s3.enabled` | Set to `true` to enable S3 uploads. |
| `STREAMFLOW_AWS_S3_BUCKET` | `streamflow.aws.s3.bucket` | S3 bucket name for images and videos. |

## Optional

| Env variable | Property | Default | Description |
|--------------|----------|---------|-------------|
| `STREAMFLOW_AWS_S3_REGION` | `streamflow.aws.s3.region` | `us-east-1` | AWS region for the bucket. |
| `STREAMFLOW_AWS_S3_ACCESS_KEY_ID` | `streamflow.aws.s3.access-key-id` | (none) | AWS access key. If unset, SDK uses default credential chain (e.g. `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` or IAM role). |
| `STREAMFLOW_AWS_S3_SECRET_ACCESS_KEY` | `streamflow.aws.s3.secret-access-key` | (none) | AWS secret key. Use only when setting access-key-id. |
| `AWS_ACCESS_KEY_ID` | (SDK default) | (none) | Standard AWS env; used when S3 access-key-id is not set. |
| `AWS_SECRET_ACCESS_KEY` | (SDK default) | (none) | Standard AWS env; used when S3 secret is not set. |
| `STREAMFLOW_AWS_S3_ENDPOINT_OVERRIDE` | `streamflow.aws.s3.endpoint-override` | (none) | Custom endpoint (e.g. LocalStack: `http://localhost:4566`). |
| `STREAMFLOW_AWS_S3_PATH_STYLE_ACCESS` | `streamflow.aws.s3.path-style-access` | `false` | Set to `true` for path-style access (e.g. LocalStack). |
| `STREAMFLOW_AWS_S3_IMAGES_PREFIX` | `streamflow.aws.s3.images-prefix` | `images/` | Key prefix for image uploads. |
| `STREAMFLOW_AWS_S3_VIDEOS_PREFIX` | `streamflow.aws.s3.videos-prefix` | `videos/` | Key prefix for video uploads. |
| `STREAMFLOW_AWS_S3_MAX_FILE_SIZE_MB` | `streamflow.aws.s3.max-file-size-mb` | `512` | Max upload size in MB (validated before upload). |

## Example (production)

```bash
export STREAMFLOW_AWS_S3_ENABLED=true
export STREAMFLOW_AWS_S3_BUCKET=my-streamflow-media
export STREAMFLOW_AWS_S3_REGION=us-east-1
# Prefer IAM role; or:
# export AWS_ACCESS_KEY_ID=AKIA...
# export AWS_SECRET_ACCESS_KEY=...
```

## Example (LocalStack)

```bash
export STREAMFLOW_AWS_S3_ENABLED=true
export STREAMFLOW_AWS_S3_BUCKET=media
export STREAMFLOW_AWS_S3_REGION=us-east-1
export STREAMFLOW_AWS_S3_ENDPOINT_OVERRIDE=http://localhost:4566
export STREAMFLOW_AWS_S3_PATH_STYLE_ACCESS=true
export STREAMFLOW_AWS_S3_ACCESS_KEY_ID=test
export STREAMFLOW_AWS_S3_SECRET_ACCESS_KEY=test
```

## Usage in code

- **`S3StorageService`** is available only when `streamflow.aws.s3.enabled=true` and the S3 client is configured.
- **Upload image:** `s3StorageService.uploadImage(multipartFile)` → returns S3 key (e.g. `images/<uuid>-filename.jpg`).
- **Upload video:** `s3StorageService.uploadVideo(multipartFile)` → returns S3 key (e.g. `videos/<uuid>-filename.mp4`).
- **Upload with custom key:** `s3StorageService.upload(key, inputStream, contentLength, contentType, metadata)`.
- **Errors:** All S3 upload/delete failures are thrown as `S3UploadException` (or `S3StorageException`) with bucket/key and cause.

## Spring property naming

Spring Boot maps env vars to properties by replacing underscores with hyphens and lowercasing, e.g. `STREAMFLOW_AWS_S3_BUCKET` → `streamflow.aws.s3.bucket`.
