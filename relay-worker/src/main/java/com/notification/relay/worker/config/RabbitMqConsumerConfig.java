package com.notification.relay.worker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConsumerConfig {

	private static final int WAIT_TTL_MS = 30000;

	// SMS ──────────────────────────────────────────────────────────────────

	@Bean
	public DirectExchange smsWaitExchange() {
		return new DirectExchange("notification.sms.wait.exchange");
	}

	@Bean
	public Queue smsWaitQueue() {
		return QueueBuilder.durable("notification.sms.wait")
				.withArgument("x-message-ttl", WAIT_TTL_MS)
				.withArgument("x-dead-letter-exchange", "notification.sms.exchange")
				.build();
	}

	@Bean
	public Queue smsDeadQueue() {
		return QueueBuilder.durable("notification.sms.dead").build();
	}

	@Bean
	public Binding smsWaitBinding(DirectExchange smsWaitExchange) {
		return BindingBuilder.bind(smsWaitQueue())
				.to(smsWaitExchange).with("notification.sms.wait");
	}

	// ── Email ─────────────────────────────────────────────────────────────────

	@Bean
	public DirectExchange emailWaitExchange() {
		return new DirectExchange("notification.email.wait.exchange");
	}

	@Bean
	public Queue emailWaitQueue() {
		return QueueBuilder.durable("notification.email.wait")
				.withArgument("x-message-ttl", WAIT_TTL_MS)
				.withArgument("x-dead-letter-exchange", "notification.email.exchange")
				.build();
	}

	@Bean
	public Queue emailDeadQueue() {
		return QueueBuilder.durable("notification.email.dead").build();
	}

	@Bean
	public Binding emailWaitBinding(DirectExchange emailWaitExchange) {
		return BindingBuilder.bind(emailWaitQueue())
				.to(emailWaitExchange).with("notification.email.wait");
	}

	// ── Kakao ─────────────────────────────────────────────────────────────────

	@Bean
	public DirectExchange kakaoWaitExchange() {
		return new DirectExchange("notification.kakao.wait.exchange");
	}

	@Bean
	public Queue kakaoWaitQueue() {
		return QueueBuilder.durable("notification.kakao.wait")
				.withArgument("x-message-ttl", WAIT_TTL_MS)
				.withArgument("x-dead-letter-exchange", "notification.kakao.exchange")
				.build();
	}

	@Bean
	public Queue kakaoDeadQueue() {
		return QueueBuilder.durable("notification.kakao.dead").build();
	}

	@Bean
	public Binding kakaoWaitBinding(DirectExchange kakaoWaitExchange) {
		return BindingBuilder.bind(kakaoWaitQueue())
				.to(kakaoWaitExchange).with("notification.kakao.wait");
	}
}
