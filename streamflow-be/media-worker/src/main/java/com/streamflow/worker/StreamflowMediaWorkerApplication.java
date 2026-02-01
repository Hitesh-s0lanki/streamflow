package com.streamflow.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Media processing worker: consumes Kafka ingestion events, downloads raw video
 * from S3, transcodes with FFmpeg (HLS/DASH), generates sprites, uploads to S3,
 * and updates DB. No web server.
 */
@SpringBootApplication
@ComponentScan(basePackages = { "com.streamflow.worker", "com.streamflow.config", "com.streamflow.repository" })
@EntityScan(basePackages = "com.streamflow.entity")
@EnableJpaRepositories(basePackages = "com.streamflow.repository")
public class StreamflowMediaWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamflowMediaWorkerApplication.class, args);
    }
}
