package com.notification.relay.infrastructure.adapter.out;

import com.notification.relay.application.port.out.NotificationSenderPort;
import com.notification.relay.core.domain.Notification;
import com.notification.relay.infrastructure.persistence.entity.NotificationOutbox;
import com.notification.relay.infrastructure.persistence.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class NotificationSenderAdapter implements NotificationSenderPort {

	private final NotificationOutboxRepository outboxRepository;

	@Override
	public void send(Notification notification) {

		NotificationOutbox outbox = NotificationOutbox.create(
				notification.getId(),
				notification.getUserId(),
				notification.getMessage(),
				notification.getNotificationType()
		);

		outboxRepository.save(outbox);
	}
}
