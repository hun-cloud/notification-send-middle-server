package com.notification.relay.worker.consumer;

import java.io.IOException;

import com.notification.relay.worker.config.RetryProperties;
import com.notification.relay.worker.idempotency.IdempotencyChecker;
import com.notification.relay.worker.publisher.DeadLetterPublisher;
import com.notification.relay.worker.sender.NotificationSender;
import com.notification.relay.worker.sender.SendRequest;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoNotificationConsumer {

	private final IdempotencyChecker idempotencyChecker;
	private final DeadLetterPublisher deadLetterPublisher;
	private final RetryProperties retryProperties;
	private final NotificationSender notificationSender;
	private final MessageParser messageParser;

	@RabbitListener(queues = {"notification.kakao.0", "notification.kakao.1",
			"notification.kakao.2", "notification.kakao.3"})
	public void handle(Message message, Channel channel) throws IOException {
		long deliveryTag = message.getMessageProperties().getDeliveryTag();
		String notificationId = messageParser.getNotificationId(message);

		try {
			if (idempotencyChecker.isAlreadyProcessed(notificationId)) {
				log.info("[Kakao] 중복 메시지 무시: notificationId={}", notificationId);
				channel.basicAck(deliveryTag, false);
				return;
			}

			notificationSender.send(new SendRequest(
					notificationId,
					"KAKAO",
					messageParser.getReceiver(message),
					messageParser.getBody(message)
			));

			idempotencyChecker.markAsProcessed(notificationId);
			channel.basicAck(deliveryTag, false);
			log.info("[Kakao] 처리 완료: notificationId={}", notificationId);

		} catch (Exception e) {
			int deathCount = messageParser.getDeathCount(message);
			log.warn("[Kakao] 처리 실패: notificationId={}, deathCount={}", notificationId, deathCount);

			if (deathCount >= retryProperties.getMaxAttempts()) {
				log.error("[Kakao] 최종 실패, Dead Queue 이동: notificationId={}", notificationId);
				deadLetterPublisher.publish(message, "kakao");
				channel.basicAck(deliveryTag, false);
			} else {
				channel.basicNack(deliveryTag, false, false);
			}
		}
	}
}
