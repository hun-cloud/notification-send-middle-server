package com.notification.relay.infrastructure.adapter.out.messaging.strategy;

import com.notification.relay.core.domain.NotificationType;

public interface NotificationRoutingKeyStrategy {
	NotificationType supportType();
	String getRoutingKey();
}
