package com.notification.relay.worker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sender.mock")
public class SenderProperties {
	private String url;
	private int connectTimeoutMs = 3000;
	private int readTimeoutMs = 5000;
}
