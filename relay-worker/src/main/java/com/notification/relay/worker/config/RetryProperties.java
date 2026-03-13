package com.notification.relay.worker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "consumer.retry")
public class RetryProperties {
	private int maxAttempts = 3;
	private int waitTtlMs = 30000;
}
