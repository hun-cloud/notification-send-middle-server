package com.notification.relay.infrastructure.adapter.out.config;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rabbitmq.notification")
public class RabbitMqProperties {

	private String exchange;
	private RoutingKey routingKey;

	@Getter
	@Setter
	public static class RoutingKey {
		private String sms;
		private String kakao;
		private String email;
	}
}
