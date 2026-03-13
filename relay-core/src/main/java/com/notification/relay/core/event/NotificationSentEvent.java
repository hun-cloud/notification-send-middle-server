package com.notification.relay.core.event;

import com.notification.relay.core.domain.NotificationType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NotificationSentEvent {

	private final String notificationId;
	private final String userId;
	private final String message;
	private final NotificationType notificationType;
}
