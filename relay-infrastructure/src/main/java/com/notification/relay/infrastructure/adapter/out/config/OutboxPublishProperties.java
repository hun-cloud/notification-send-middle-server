package com.notification.relay.infrastructure.adapter.out.config;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "outbox.publish")
public class OutboxPublishProperties {
	private long confirmTimeoutSeconds = 5;
	private int maxRetryCount = 3;
}
