package com.notification.relay.application.dto.command;

import com.notification.relay.core.domain.NotificationType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SendNotificationCommand {
	private final String userId;
	private final String message;
	private final NotificationType notificationType;

	public static SendNotificationCommand of(String userId, String message, NotificationType notificationType) {
		return new SendNotificationCommand(userId, message, notificationType);
	}
}
