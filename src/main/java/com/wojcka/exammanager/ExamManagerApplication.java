package com.wojcka.exammanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync(proxyTargetClass = true)
public class ExamManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExamManagerApplication.class, args);
	}

	@Bean
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(2);
		executor.setKeepAliveSeconds(180);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("QuestionProcess-");
		executor.initialize();
		return executor;
	}
}
