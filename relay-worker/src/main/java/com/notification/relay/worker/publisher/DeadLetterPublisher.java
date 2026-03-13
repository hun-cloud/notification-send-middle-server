package com.notification.relay.worker.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeadLetterPublisher {

	private final RabbitTemplate rabbitTemplate;

	public void publish(Message message, String type) {
		String deadQueueName = "notification." + type + ".dead";
		rabbitTemplate.send("", deadQueueName, message);
	}
}
