package com.notification.relay.worker.config;

import java.time.Duration;
import java.util.Map;

import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ResilienceConfig {

	private final RetryClassifier retryClassifier;
	private final HttpRetryProperties httpRetryProperties;

	@Bean
	public RetryRegistry retryRegistry() {
		RetryConfig config = RetryConfig.custom()
				.maxAttempts(httpRetryProperties.getMaxAttempts())
				.waitDuration(Duration.ofMillis(httpRetryProperties.getWaitDurationMs()))
				.retryOnException(retryClassifier::isRetryable)
				.build();

		return RetryRegistry.of(
				Map.of("notificationSender", config)
		);
	}
}
