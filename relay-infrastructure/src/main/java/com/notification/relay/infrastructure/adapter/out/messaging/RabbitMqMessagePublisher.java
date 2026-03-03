package com.notification.relay.infrastructure.adapter.out.messaging;

import java.util.concurrent.TimeUnit;

import com.notification.relay.application.port.out.MessagePublisherPort;
import com.notification.relay.core.domain.Notification;
import com.notification.relay.infrastructure.adapter.out.config.OutboxPublishProperties;
import com.notification.relay.infrastructure.exception.MessageRoutingException;
import lombok.RequiredArgsConstructor;

import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitMqMessagePublisher implements MessagePublisherPort {

	private final RabbitTemplate rabbitTemplate;
	private final RoutingKeyStrategyFactory routingKeyStrategyFactory;
	private final OutboxPublishProperties publishProperties;


	@Override
	public void publish(Notification notification) {
		String routingKey = routingKeyStrategyFactory
				.getStrategy(notification.getNotificationType())
				.getRoutingKey();
		String exchangeName = routingKey + ".exchange";

		CorrelationData correlationData = new CorrelationData(
				String.valueOf(notification.getId()));

		rabbitTemplate.convertAndSend(exchangeName, routingKey, notification.getMessage(),
				message -> {
					message.getMessageProperties()
							.setHeader("notificationId", notification.getId());
					return message;
				},
				correlationData);

		CorrelationData.Confirm confirm = correlationData.getFuture()
				.orTimeout(publishProperties.getConfirmTimeoutSeconds(), TimeUnit.SECONDS)
				.join();

		if (!confirm.ack()) {
			throw new MessageRoutingException("메시지 발행 실패: " + confirm.reason());
		}

		ReturnedMessage returned = correlationData.getReturned();
		if (returned != null) {
			throw new MessageRoutingException(
					"라우팅 실패: replyCode=" + returned.getReplyCode()
							+ ", replyText=" + returned.getReplyText()
			);
		}
	}
}
