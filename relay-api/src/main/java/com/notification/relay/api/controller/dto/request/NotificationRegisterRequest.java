package com.notification.relay.api.controller.dto.request;

import com.notification.relay.application.dto.command.SendNotificationCommand;
import com.notification.relay.core.domain.NotificationType;

public record NotificationRegisterRequest(
		String userId,
		String message,
		NotificationType notificationType
) {
	public SendNotificationCommand toCommand() {
		return SendNotificationCommand.of(userId, message, notificationType);
	}
}
