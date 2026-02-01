package com.streamflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class StreamflowAppBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(StreamflowAppBeApplication.class, args);
	}

}
