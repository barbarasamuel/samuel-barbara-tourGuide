package com.openclassrooms.tourguide;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootTest
@EnableAsync
class TourguideApplicationTests {

	@Test
	void contextLoads() {
	}
	public static void main(String[] args) {
		SpringApplication.run(TourguideApplication.class, args);
	}

	/*@Bean(name = "taskExecutor")
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(50);
		executor.setQueueCapacity(99950);
		executor.setThreadNamePrefix("Task-");
		executor.initialize();
		return executor;
	}*/
}
