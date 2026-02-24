package com.streamflow;

import com.streamflow.service.S3StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(StreamflowApplicationTests.S3StorageTestConfig.class)
class StreamflowApplicationTests {

	@Test
	void contextLoads() {
	}

	@TestConfiguration
	static class S3StorageTestConfig {
		@Bean
		@ConditionalOnMissingBean(S3StorageService.class)
		S3StorageService s3StorageService() {
			return org.mockito.Mockito.mock(S3StorageService.class);
		}
	}
}
