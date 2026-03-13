package com.notification.relay.relay.worker.consumer;

import java.io.IOException;

import com.notification.relay.core.domain.NotificationType;
import com.notification.relay.core.event.NotificationFailedEvent;
import com.notification.relay.core.event.NotificationSentEvent;
import com.notification.relay.worker.config.RetryProperties;
import com.notification.relay.worker.consumer.MessageParser;
import com.notification.relay.worker.idempotency.IdempotencyChecker;
import com.notification.relay.worker.publisher.DeadLetterPublisher;
import com.notification.relay.worker.sender.NotificationSender;
import com.notification.relay.worker.sender.SendRequest;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationConsumer {

	private final IdempotencyChecker idempotencyChecker;
	private final DeadLetterPublisher deadLetterPublisher;
	private final RetryProperties retryProperties;
	private final NotificationSender notificationSender;
	private final MessageParser messageParser;
	private final ApplicationEventPublisher eventPublisher;

	@RabbitListener(queues = {"notification.email.0", "notification.email.1",
			"notification.email.2", "notification.email.3"})
	public void handle(Message message, Channel channel) throws IOException {
		long deliveryTag = message.getMessageProperties().getDeliveryTag();
		String notificationId = messageParser.getNotificationId(message);

		try {
			if (idempotencyChecker.isAlreadyProcessed(notificationId)) {
				log.info("[Email] 중복 메시지 무시: notificationId={}", notificationId);
				channel.basicAck(deliveryTag, false);
				return;
			}

			notificationSender.send(new SendRequest(
					notificationId,
					"EMAIL",
					messageParser.getReceiver(message),
					messageParser.getBody(message)
			));

			idempotencyChecker.markAsProcessed(notificationId);
			channel.basicAck(deliveryTag, false);

			eventPublisher.publishEvent(new NotificationSentEvent(
					notificationId,
					messageParser.getReceiver(message),
					messageParser.getBody(message),
					NotificationType.EMAIL
			));
			log.info("[Email] 처리 완료: notificationId={}", notificationId);

		} catch (Exception e) {
			int deathCount = messageParser.getDeathCount(message);
			log.warn("[Email] 처리 실패: notificationId={}, deathCount={}", notificationId, deathCount);

			if (deathCount >= retryProperties.getMaxAttempts()) {
				log.error("[Email] 최종 실패, Dead Queue 이동: notificationId={}", notificationId);
				deadLetterPublisher.publish(message, "email");
				channel.basicAck(deliveryTag, false);

				eventPublisher.publishEvent(new NotificationFailedEvent(
						notificationId,
						messageParser.getReceiver(message),
						messageParser.getBody(message),
						NotificationType.EMAIL,
						e.getMessage()
				));
			} else {
				channel.basicNack(deliveryTag, false, false);
			}
		}
	}
}
