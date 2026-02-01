package com.streamflow.service;

import com.streamflow.exception.S3UploadException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;
    @Mock
    private S3Presigner s3Presigner;

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("throws when bucket not configured")
        void throwsWhenBucketNotConfigured() {
            S3StorageService service = new S3StorageService(s3Client, s3Presigner);
            // bucket is empty by default when not in Spring context
            ReflectionTestUtils.setField(service, "bucket", "");
            InputStream in = new ByteArrayInputStream(new byte[10]);

            assertThatThrownBy(() -> service.upload("key", in, 10, "application/octet-stream", null))
                    .isInstanceOf(S3UploadException.class)
                    .hasMessageContaining("bucket");
        }

        @Test
        @DisplayName("throws when key is blank")
        void throwsWhenKeyBlank() {
            S3StorageService service = new S3StorageService(s3Client, s3Presigner);
            ReflectionTestUtils.setField(service, "bucket", "test-bucket");
            InputStream in = new ByteArrayInputStream(new byte[1]);

            assertThatThrownBy(() -> service.upload(" ", in, 1, "application/octet-stream", null))
                    .isInstanceOf(S3UploadException.class)
                    .hasMessageContaining("key");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("throws when bucket not configured")
        void throwsWhenBucketNotConfigured() {
            S3StorageService service = new S3StorageService(s3Client, s3Presigner);
            ReflectionTestUtils.setField(service, "bucket", "");

            assertThatThrownBy(() -> service.delete("some/key"))
                    .isInstanceOf(S3UploadException.class)
                    .hasMessageContaining("bucket");
        }
    }

    @Nested
    @DisplayName("generateRawVideoKey")
    class GenerateRawVideoKey {

        @Test
        @DisplayName("returns key with video asset id and uuid")
        void returnsKeyWithVideoAssetIdAndUuid() {
            S3StorageService service = new S3StorageService(s3Client, s3Presigner);
            ReflectionTestUtils.setField(service, "videosPrefix", "videos/");

            String key = service.generateRawVideoKey(java.util.UUID.fromString("22222222-2222-2222-2222-222222222222"));

            assertThat(key).startsWith("videos/raw/22222222-2222-2222-2222-222222222222/");
            assertThat(key).matches("videos/raw/22222222-2222-2222-2222-222222222222/[0-9a-f-]{36}");
        }
    }
}
