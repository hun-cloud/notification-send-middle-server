package com.notification.relay.infrastructure.adapter.out.secheduler;

import java.time.LocalDateTime;
import java.util.List;

import com.notification.relay.core.domain.NotificationType;
import com.notification.relay.infrastructure.adapter.out.config.RabbitMqProperties;
import com.notification.relay.infrastructure.adapter.out.messaging.RoutingKeyStrategyFactory;
import com.notification.relay.infrastructure.adapter.out.messaging.strategy.NotificationRoutingKeyStrategy;
import com.notification.relay.infrastructure.persistence.entity.NotificationOutbox;
import com.notification.relay.infrastructure.persistence.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationOutboxPublisherTests {

	@Mock
	private NotificationOutboxRepository outboxRepository;

	@Mock
	private RabbitTemplate rabbitTemplate;

	@Mock
	private RabbitMqProperties rabbitMqProperties;

	@Mock
	private RoutingKeyStrategyFactory routingKeyStrategyFactory;

	@Mock
	private NotificationRoutingKeyStrategy smsStrategy;

	@InjectMocks
	private NotificationOutboxPublisher publisher;

	private static final String SMS_ROUTING_KEY = "notification.sms";
	private static final String SMS_EXCHANGE    = "notification.sms.exchange";

	@BeforeEach
	void setUp() {
		given(routingKeyStrategyFactory.getStrategy(NotificationType.SMS)).willReturn(smsStrategy);
		given(smsStrategy.getRoutingKey()).willReturn(SMS_ROUTING_KEY);
	}

	// ── 헬퍼 ──────────────────────────────────────────────────────────

	private NotificationOutbox createOutbox(Long id, String userId) {
		return NotificationOutbox.create(id, userId, "테스트 메시지", NotificationType.SMS);
	}

	/** rabbitTemplate.convertAndSend 호출 시 즉시 ACK/NACK 완료 처리 */
	private void givenRabbitAck(boolean ack) {
		willAnswer(invocation -> {
			CorrelationData correlationData = invocation.getArgument(4);
			correlationData.getFuture().complete(new CorrelationData.Confirm(ack, ack ? null : "broker rejected"));
			return null;
		}).given(rabbitTemplate).convertAndSend(
				anyString(), anyString(), any(), any(MessagePostProcessor.class), any(CorrelationData.class)
		);
	}

	// ── publishPendingMessages ─────────────────────────────────────────

	@Test
	@DisplayName("PENDING 메시지가 여러 개이면 각각 발행한다")
	void publishPendingMessages_publishes_all_pending() {
		// given
		NotificationOutbox outbox1 = createOutbox(1L, "userA");
		NotificationOutbox outbox2 = createOutbox(2L, "userB");

		given(outboxRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
				eq(NotificationOutbox.OutBoxStatus.PENDING),
				any(LocalDateTime.class),
				any(PageRequest.class)
		)).willReturn(List.of(outbox1, outbox2));

		givenRabbitAck(true);

		// when
		publisher.publishPendingMessages();

		// then
		then(rabbitTemplate).should(times(2)).convertAndSend(
				anyString(), anyString(), any(), any(MessagePostProcessor.class), any(CorrelationData.class)
		);
	}

	@Test
	@DisplayName("PENDING 메시지가 없으면 발행하지 않는다")
	void publishPendingMessages_does_nothing_when_no_pending() {
		// given
		given(outboxRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
				any(), any(), any()
		)).willReturn(List.of());

		// when
		publisher.publishPendingMessages();

		// then
		then(rabbitTemplate).should(never()).convertAndSend(
				anyString(), anyString(), any(), any(MessagePostProcessor.class), any(CorrelationData.class)
		);
	}

	// ── publishSingleMessage: 성공 ─────────────────────────────────────

	@Test
	@DisplayName("ACK를 받으면 상태가 PUBLISHED로 변경되고 저장된다")
	void publishSingleMessage_ack_marks_as_published() {
		// given
		NotificationOutbox outbox = createOutbox(1L, "userA");
		givenRabbitAck(true);

		// when
		publisher.publishSingleMessage(outbox);

		// then
		assertThat(outbox.getStatus()).isEqualTo(NotificationOutbox.OutBoxStatus.PUBLISHED);
		then(outboxRepository).should(times(1)).save(outbox);
	}

	@Test
	@DisplayName("올바른 Exchange 이름으로 발행한다 (routingKey + '.exchange')")
	void publishSingleMessage_sends_to_correct_exchange() {
		// given
		NotificationOutbox outbox = createOutbox(1L, "userA");
		ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);

		willAnswer(invocation -> {
			CorrelationData cd = invocation.getArgument(4);
			cd.getFuture().complete(new CorrelationData.Confirm(true, null));
			return null;
		}).given(rabbitTemplate).convertAndSend(
				exchangeCaptor.capture(), anyString(), any(), any(MessagePostProcessor.class), any(CorrelationData.class)
		);

		// when
		publisher.publishSingleMessage(outbox);

		// then
		assertThat(exchangeCaptor.getValue()).isEqualTo(SMS_EXCHANGE);
	}

	@Test
	@DisplayName("CorrelationData의 ID는 outbox ID와 일치한다")
	void publishSingleMessage_correlation_id_matches_outbox_id() {
		// given
		long outboxId = 999L;
		NotificationOutbox outbox = createOutbox(outboxId, "userA");
		ArgumentCaptor<CorrelationData> correlationCaptor = ArgumentCaptor.forClass(CorrelationData.class);

		willAnswer(invocation -> {
			CorrelationData cd = invocation.getArgument(4);
			cd.getFuture().complete(new CorrelationData.Confirm(true, null));
			return null;
		}).given(rabbitTemplate).convertAndSend(
				anyString(), anyString(), any(), any(MessagePostProcessor.class), correlationCaptor.capture()
		);

		// when
		publisher.publishSingleMessage(outbox);

		// then
		assertThat(correlationCaptor.getValue().getId()).isEqualTo(String.valueOf(outboxId));
	}

	@Test
	@DisplayName("MessagePostProcessor에 notificationId 헤더가 포함된다")
	void publishSingleMessage_includes_notification_id_header() {
		// given
		long outboxId = 1L;
		NotificationOutbox outbox = createOutbox(outboxId, "userA");
		ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);

		willAnswer(invocation -> {
			CorrelationData cd = invocation.getArgument(4);
			cd.getFuture().complete(new CorrelationData.Confirm(true, null));
			return null;
		}).given(rabbitTemplate).convertAndSend(
				anyString(), anyString(), any(), processorCaptor.capture(), any(CorrelationData.class)
		);

		// when
		publisher.publishSingleMessage(outbox);

		// then: 캡처한 MessagePostProcessor를 실제 메시지에 적용하여 헤더 검증
		MessageProperties props = new MessageProperties();
		Message message = new Message("body".getBytes(), props);
		processorCaptor.getValue().postProcessMessage(message);

		assertThat((Long) message.getMessageProperties().getHeader("notificationId"))
				.isEqualTo(outboxId);
	}

	// ── publishSingleMessage: 실패/재시도 ────────────────────────────────

	@Test
	@DisplayName("NACK를 받고 재시도 가능하면 retryCount가 증가한다")
	void publishSingleMessage_nack_increments_retry_when_can_retry() {
		// given
		NotificationOutbox outbox = createOutbox(1L, "userA"); // retryCount = 0
		givenRabbitAck(false);

		// when
		publisher.publishSingleMessage(outbox);

		// then
		assertThat(outbox.getRetryCount()).isEqualTo(1);
		assertThat(outbox.getStatus()).isEqualTo(NotificationOutbox.OutBoxStatus.PENDING);
		then(outboxRepository).should(times(1)).save(outbox);
	}

	@Test
	@DisplayName("재시도 횟수 초과 시 상태가 FAILED로 변경된다")
	void publishSingleMessage_nack_marks_failed_when_retry_exhausted() {
		// given
		NotificationOutbox outbox = createOutbox(1L, "userA");
		outbox.incrementRetry("1차 실패");
		outbox.incrementRetry("2차 실패");
		outbox.incrementRetry("3차 실패"); // retryCount = 3, canRetry() = false

		givenRabbitAck(false);

		// when
		publisher.publishSingleMessage(outbox);

		// then
		assertThat(outbox.getStatus()).isEqualTo(NotificationOutbox.OutBoxStatus.FAILED);
		then(outboxRepository).should(times(1)).save(outbox);
	}

	@Test
	@DisplayName("OptimisticLockingFailureException 발생 시 무시하고 저장하지 않는다")
	void publishSingleMessage_ignores_optimistic_locking_exception() {
		// given
		NotificationOutbox outbox = createOutbox(1L, "userA");

		willThrow(OptimisticLockingFailureException.class)
				.given(rabbitTemplate).convertAndSend(
						anyString(), anyString(), any(), any(MessagePostProcessor.class), any(CorrelationData.class)
				);

		// when (예외 없이 정상 종료되어야 함)
		publisher.publishSingleMessage(outbox);

		// then
		then(outboxRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("타임아웃 등 일반 Exception 발생 시 재시도 가능하면 retryCount가 증가한다")
	void publishSingleMessage_exception_increments_retry_when_can_retry() {
		// given
		NotificationOutbox outbox = createOutbox(1L, "userA");

		willThrow(new RuntimeException("타임아웃"))
				.given(rabbitTemplate).convertAndSend(
						anyString(), anyString(), any(), any(MessagePostProcessor.class), any(CorrelationData.class)
				);

		// when
		publisher.publishSingleMessage(outbox);

		// then
		assertThat(outbox.getRetryCount()).isEqualTo(1);
		assertThat(outbox.getErrorMessage()).isEqualTo("타임아웃");
		then(outboxRepository).should(times(1)).save(outbox);
	}
}
