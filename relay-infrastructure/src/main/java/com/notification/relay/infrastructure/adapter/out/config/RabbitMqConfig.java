package com.notification.relay.infrastructure.adapter.out.config;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

	private final RabbitMqProperties properties;

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMandatory(true);
		rabbitTemplate.setReturnsCallback(returned -> {
			log.error("메시지 라우팅 실패: exchange={}, routingKey={}, replyCode={}, replyText={}",
					returned.getExchange(),
					returned.getRoutingKey(),
					returned.getReplyCode(),
					returned.getReplyText());
		});
		return rabbitTemplate;
	}

	// SMS
	@Bean
	public CustomExchange smsExchange() {
		return consistentHashExchange(properties.getRoutingKey().getSms() + ".exchange");
	}

	@Bean
	public Queue smsQueue0() {
		return new Queue(properties.getRoutingKey().getSms() + ".0", true);
	}

	@Bean
	public Queue smsQueue1() {
		return new Queue(properties.getRoutingKey().getSms() + ".1", true);
	}

	@Bean
	public Binding smsBinding0() {
		return bind(smsQueue0(), smsExchange());
	}

	@Bean
	public Binding smsBinding1() {
		return bind(smsQueue1(), smsExchange());
	}

	// Email
	@Bean
	public CustomExchange emailExchange() {
		return consistentHashExchange(properties.getRoutingKey().getEmail() + ".exchange");
	}

	@Bean public Queue emailQueue0() {
		return new Queue(properties.getRoutingKey().getEmail() + ".0", true);
	}

	@Bean public Queue emailQueue1() {
		return new Queue(properties.getRoutingKey().getEmail() + ".1", true);
	}

	@Bean public Queue emailQueue2() {
		return new Queue(properties.getRoutingKey().getEmail() + ".2", true);
	}

	@Bean public Queue emailQueue3() {
		return new Queue(properties.getRoutingKey().getEmail() + ".3", true);
	}

	@Bean public Binding emailBinding0(Queue emailQueue0, CustomExchange emailExchange) {
		return bind(emailQueue0, emailExchange);
	}

	@Bean public Binding emailBinding1(Queue emailQueue1, CustomExchange emailExchange) {
		return bind(emailQueue1, emailExchange);
	}

	@Bean public Binding emailBinding2(Queue emailQueue2, CustomExchange emailExchange) {
		return bind(emailQueue2, emailExchange);
	}

	@Bean public Binding emailBinding3(Queue emailQueue3, CustomExchange emailExchange) {
		return
			bind(emailQueue3, emailExchange);
	}

	// kakao

	@Bean
	public CustomExchange kakaoExchange() {
		return consistentHashExchange(properties.getRoutingKey().getKakao() + ".exchange");
	}

	@Bean public Queue kakaoQueue0() {
		return new Queue(properties.getRoutingKey().getKakao() + ".0", true);
	}

	@Bean public Queue kakaoQueue1() {
		return new Queue(properties.getRoutingKey().getKakao() + ".1", true);
	}

	@Bean public Queue kakaoQueue2() {
		return new Queue(properties.getRoutingKey().getKakao() + ".2", true);
	}

	@Bean public Queue kakaoQueue3() {
		return new Queue(properties.getRoutingKey().getKakao() + ".3", true);
	}

	@Bean public Binding kakaoBinding0(Queue kakaoQueue0, CustomExchange kakaoExchange) {
		return bind(kakaoQueue0, kakaoExchange);
	}

	@Bean public Binding kakaoBinding1(Queue kakaoQueue1, CustomExchange kakaoExchange) {
		return bind(kakaoQueue1, kakaoExchange);
	}

	@Bean public Binding kakaoBinding2(Queue kakaoQueue2, CustomExchange kakaoExchange) {
		return bind(kakaoQueue2, kakaoExchange);
	}

	@Bean public Binding kakaoBinding3(Queue kakaoQueue3, CustomExchange kakaoExchange) {
		return bind(kakaoQueue3, kakaoExchange);
	}

	private CustomExchange consistentHashExchange(String name) {
		Map<String, Object> args = new HashMap<>();
		args.put("hash-header", "userId");
		return new CustomExchange(name, "x-consistent-hash", true, false, args);
	}

	private Binding bind(Queue queue, CustomExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with("1").noargs();
	}
}
