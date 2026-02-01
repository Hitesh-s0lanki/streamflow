package com.streamflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kafka-related app config (ingestion topic). Bootstrap servers come from
 * spring.kafka.bootstrap-servers.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {

    /**
     * Topic to which ingestion job events are published for the media worker.
     */
    private String ingestionTopic = "streamflow.ingestion.jobs";
}
