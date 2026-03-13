package com.notification.relay.worker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sender.retry")
public class HttpRetryProperties {
	private int maxAttempts = 3;
	private long waitDurationMs = 1000;
}
