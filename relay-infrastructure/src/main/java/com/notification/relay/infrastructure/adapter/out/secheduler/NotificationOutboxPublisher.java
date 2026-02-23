package com.notification.relay.infrastructure.adapter.out.secheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.notification.relay.infrastructure.adapter.out.config.RabbitMqProperties;
import com.notification.relay.infrastructure.adapter.out.messaging.RoutingKeyStrategyFactory;
import com.notification.relay.infrastructure.persistence.entity.NotificationOutbox;
import com.notification.relay.infrastructure.persistence.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
	private final RabbitTemplate rabbitTemplate;
	private final RabbitMqProperties rabbitMqProperties;
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
			String routingKey = routingKeyStrategyFactory
					.getStrategy(outbox.getNotificationType())
					.getRoutingKey();

			CorrelationData correlationData = new CorrelationData(
					String.valueOf(outbox.getId())
			);

			rabbitTemplate.convertAndSend(
					rabbitMqProperties.getExchange(),
					routingKey,
					outbox.getMessage(),
					correlationData
			);

			CorrelationData.Confirm confirm = correlationData
					.getFuture()
					.get(5, TimeUnit.SECONDS);

			if (!confirm.ack()) {
				throw new RuntimeException("메시지 발행 실패: " + confirm.reason());
			}

			outbox.markAsPublished();
			outboxRepository.save(outbox);

		} catch (OptimisticLockingFailureException e) {
			// 다른 프로세스가 이미 처리했을 수 있으므로 무시
		} catch (Exception e) {
			if (outbox.canRetry()) {
				outbox.incrementRetry(e.getMessage());
			} else {
				outbox.markAsFailed(e.getMessage());
			}
			outboxRepository.save(outbox);
		}
	}
}
