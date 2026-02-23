package com.notification.relay.infrastructure.adapter.out.config;

import lombok.RequiredArgsConstructor;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

	private final RabbitMqProperties properties;

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMandatory(true);
		return rabbitTemplate;
	}

	@Bean
	public TopicExchange notificationExchange() {
		return new TopicExchange(properties.getExchange(), true, false);
	}

	@Bean
	public Queue smsQueue() {
		return new Queue(properties.getRoutingKey().getSms(), true);
	}

	@Bean
	public Queue emailQueue() {
		return new Queue(properties.getRoutingKey().getEmail(), true);
	}

	@Bean
	public Queue kakaoQueue() {
		return new Queue(properties.getRoutingKey().getKakao(), true);
	}

	@Bean
	public Binding smsBinding() {
		return BindingBuilder
				.bind(smsQueue())
				.to(notificationExchange())
				.with(properties.getRoutingKey().getSms());
	}

	@Bean
	public Binding emailBinding() {
		return BindingBuilder
				.bind(emailQueue())
				.to(notificationExchange())
				.with(properties.getRoutingKey().getEmail());
	}

	@Bean
	public Binding kakaoBinding() {
		return BindingBuilder
				.bind(kakaoQueue())
				.to(notificationExchange())
				.with(properties.getRoutingKey().getKakao());
	}
}
