package com.notification.relay.worker.consumer;

import com.notification.relay.worker.config.RetryProperties;
import com.notification.relay.worker.idempotency.IdempotencyChecker;
import com.notification.relay.worker.publisher.DeadLetterPublisher;
import com.notification.relay.worker.sender.NotificationSender;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SmsNotificationConsumerTest {

	@Mock
	private IdempotencyChecker idempotencyChecker;

	@Mock
	private DeadLetterPublisher deadLetterPublisher;

	@Mock
	private RetryProperties retryProperties;

	@Mock
	private NotificationSender notificationSender;

	@Mock
	private MessageParser messageParser;

	@Mock
	private Channel channel;

	@InjectMocks
	private SmsNotificationConsumer consumer;

	private Message message;

	@BeforeEach
	void setUp() {
		MessageProperties props = new MessageProperties();
		props.setDeliveryTag(1L);
		message = new Message("테스트 메시지".getBytes(), props);

		given(messageParser.getNotificationId(message)).willReturn("noti-123");
	}

	@Test
	@DisplayName("정상 처리 시 발송 후 ack를 보낸다")
	void handle_success() throws IOException {
		// given
		given(idempotencyChecker.isAlreadyProcessed("noti-123")).willReturn(false);
		given(messageParser.getReceiver(message)).willReturn("user-456");
		given(messageParser.getBody(message)).willReturn("테스트 메시지");

		// when
		consumer.handle(message, channel);

		// then
		then(notificationSender).should(times(1)).send(any());
		then(idempotencyChecker).should(times(1)).markAsProcessed("noti-123");
		then(channel).should(times(1)).basicAck(1L, false);
	}

	@Test
	@DisplayName("중복 메시지는 발송하지 않고 ack를 보낸다")
	void handle_duplicate_message() throws IOException {
		// given
		given(idempotencyChecker.isAlreadyProcessed("noti-123")).willReturn(true);

		// when
		consumer.handle(message, channel);

		// then
		then(notificationSender).should(never()).send(any());
		then(idempotencyChecker).should(never()).markAsProcessed(anyString());
		then(channel).should(times(1)).basicAck(1L, false);
	}

	@Test
	@DisplayName("처리 실패 시 재시도 횟수 미만이면 nack를 보낸다")
	void handle_failure_under_max_attempts() throws IOException {
		// given
		given(idempotencyChecker.isAlreadyProcessed("noti-123")).willReturn(false);
		given(messageParser.getReceiver(message)).willReturn("user-456");
		given(messageParser.getBody(message)).willReturn("테스트 메시지");
		given(messageParser.getDeathCount(message)).willReturn(1);
		given(retryProperties.getMaxAttempts()).willReturn(3);
		willThrow(new RuntimeException("외부 API 오류")).given(notificationSender).send(any());

		// when
		consumer.handle(message, channel);

		// then
		then(channel).should(times(1)).basicNack(1L, false, false);
		then(channel).should(never()).basicAck(1L, false);
		then(deadLetterPublisher).should(never()).publish(any(), anyString());
	}

	@Test
	@DisplayName("재시도 횟수 초과 시 Dead Queue로 이동 후 ack를 보낸다")
	void handle_failure_exceed_max_attempts() throws IOException {
		// given
		given(idempotencyChecker.isAlreadyProcessed("noti-123")).willReturn(false);
		given(messageParser.getReceiver(message)).willReturn("user-456");
		given(messageParser.getBody(message)).willReturn("테스트 메시지");
		given(messageParser.getDeathCount(message)).willReturn(3);
		given(retryProperties.getMaxAttempts()).willReturn(3);
		willThrow(new RuntimeException("외부 API 오류")).given(notificationSender).send(any());

		// when
		consumer.handle(message, channel);

		// then
		then(deadLetterPublisher).should(times(1)).publish(message, "sms");
		then(channel).should(times(1)).basicAck(1L, false);
		then(channel).should(never()).basicNack(eq(1L), eq(false), eq(false));
	}
}
