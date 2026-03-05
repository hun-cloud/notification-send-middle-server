package com.notification.relay.infrastructure.adapter.out.messaging;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.notification.relay.core.domain.NotificationType;
import com.notification.relay.infrastructure.adapter.out.messaging.strategy.NotificationRoutingKeyStrategy;

import org.springframework.stereotype.Component;

@Component
public class RoutingKeyStrategyFactory {

	private final Map<NotificationType, NotificationRoutingKeyStrategy> strategies;

	public RoutingKeyStrategyFactory(List<NotificationRoutingKeyStrategy> strategies) {
		this.strategies = strategies.stream()
				.collect(
						Collectors.toMap(
								NotificationRoutingKeyStrategy::supportType,
								Function.identity()
						)
				);
	}

	public NotificationRoutingKeyStrategy getStrategy(NotificationType type) {
		return Optional.ofNullable(strategies.get(type))
				.orElseThrow(() -> new IllegalArgumentException("지원하지 않는 타입입니다. type: " + type));
	}
}
