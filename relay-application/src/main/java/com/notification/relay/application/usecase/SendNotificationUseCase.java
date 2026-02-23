package com.notification.relay.application.usecase;

import com.notification.relay.application.dto.command.SendNotificationCommand;

public interface SendNotificationUseCase {

	void execute(SendNotificationCommand command);
}
