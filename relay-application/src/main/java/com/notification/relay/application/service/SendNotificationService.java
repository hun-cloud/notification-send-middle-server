package com.notification.relay.application.service;

import com.notification.relay.application.dto.command.SendNotificationCommand;
import com.notification.relay.application.port.out.NotificationSenderPort;
import com.notification.relay.application.usecase.SendNotificationUseCase;
import com.notification.relay.common.IdGenerator;
import com.notification.relay.core.domain.Notification;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SendNotificationService implements SendNotificationUseCase {

	private final NotificationSenderPort notificationSenderPort;
	private final IdGenerator idGenerator;

	@Override
	public void execute(SendNotificationCommand command) {

		long notificationId = idGenerator.generate();
		Notification newNotification = Notification.create(notificationId, command.getUserId(), command.getMessage(), command.getNotificationType());

		notificationSenderPort.send(newNotification);
	}
}
