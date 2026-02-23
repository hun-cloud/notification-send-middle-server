package com.notification.relay.infrastructure.adapter.out.messaging.strategy;

import com.notification.relay.core.domain.NotificationType;
import com.notification.relay.infrastructure.adapter.out.config.RabbitMqProperties;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmsRoutingKeyStrategy implements NotificationRoutingKeyStrategy {

	private final RabbitMqProperties rabbitMqProperties;

	@Override
	public NotificationType supportType() {
		return NotificationType.SMS;
	}

	@Override
	public String getRoutingKey() {
		return rabbitMqProperties.getRoutingKey().getSms();
	}
}
