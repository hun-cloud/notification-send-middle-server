package com.notification.relay.api.controller.dto.request;

import com.notification.relay.application.dto.command.SendNotificationCommand;

public record NotificationRegisterRequest() {

	public SendNotificationCommand toCommand() {
		return new SendNotificationCommand();
	}
}
