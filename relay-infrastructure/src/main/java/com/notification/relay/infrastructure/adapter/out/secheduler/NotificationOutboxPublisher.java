package com.notification.relay.infrastructure.adapter.out.secheduler;

import java.time.LocalDateTime;
import java.util.List;

import com.notification.relay.application.port.out.MessagePublisherPort;
import com.notification.relay.core.domain.Notification;
import com.notification.relay.infrastructure.adapter.out.config.OutboxPublishProperties;
import com.notification.relay.infrastructure.adapter.out.messaging.RoutingKeyStrategyFactory;
import com.notification.relay.infrastructure.persistence.entity.NotificationOutbox;
import com.notification.relay.infrastructure.persistence.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class NotificationOutboxPublisher {

	private final NotificationOutboxRepository outboxRepository;
	private final MessagePublisherPort messagePublisherPort;
	private final OutboxPublishProperties publishProperties;

	private final RoutingKeyStrategyFactory routingKeyStrategyFactory;

	@Scheduled(fixedDelay = 1000)
	public void publishPendingMessages() {
		LocalDateTime threshold = LocalDateTime.now().minusSeconds(1);
		PageRequest limit = PageRequest.of(0, 100);

		List<NotificationOutbox> pendingList = outboxRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
				NotificationOutbox.OutBoxStatus.PENDING,
				threshold,
				limit
		);

		for (NotificationOutbox outbox : pendingList) {
			publishSingleMessage(outbox);
		}
	}

	@Transactional
	public void publishSingleMessage(NotificationOutbox outbox) {

		try {

			Notification newNotification = Notification.create(
					outbox.getId(),
					outbox.getUserId(),
					outbox.getMessage(),
					outbox.getNotificationType()
			);

			messagePublisherPort.publish(newNotification);

			outbox.markAsPublished();
			outboxRepository.save(outbox);

		} catch (OptimisticLockingFailureException e) {
			// 다른 프로세스가 이미 처리했을 수 있으므로 무시
		} catch (Exception e) {
			if (outbox.canRetry(publishProperties.getMaxRetryCount())) {
				outbox.incrementRetry(e.getMessage());
			} else {
				outbox.markAsFailed(e.getMessage());
			}
			outboxRepository.save(outbox);
		}
	}
}
