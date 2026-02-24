package com.streamflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class StreamflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(StreamflowApplication.class, args);
	}

}
