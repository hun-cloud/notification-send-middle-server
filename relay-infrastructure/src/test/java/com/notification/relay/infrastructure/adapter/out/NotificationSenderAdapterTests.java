package com.notification.relay.infrastructure.adapter.out;

import com.notification.relay.core.domain.Notification;
import com.notification.relay.core.domain.NotificationType;
import com.notification.relay.infrastructure.persistence.entity.NotificationOutbox;
import com.notification.relay.infrastructure.persistence.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class NotificationSenderAdapterTests {

	@Mock
	private NotificationOutboxRepository outboxRepository;

	@InjectMocks
	private NotificationSenderAdapter notificationSenderAdapter;

	@Test
	@DisplayName("Notification을 Outbox에 저장")
	void send_saves_to_outbox() {
	    //given
		long id = 123456789L;
		String userId = "user123";
		String message = "테스트 메시지";
		NotificationType type = NotificationType.SMS;

		Notification notification = Notification.create(id, userId, message, type);

		given(outboxRepository.save(any(NotificationOutbox.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

	    //when
		notificationSenderAdapter.send(notification);

	    //then
		then(outboxRepository).should(times(1)).save(any(NotificationOutbox.class));
	}

	@Test
	@DisplayName("Notification 정보가 outbox에 정확히 매핑됨")
	void send_maps_notification_to_outbox_correctly() {
		// given
		long id = 123456789L;
		String userId = "user123";
		String message = "테스트 메시지";
		NotificationType type = NotificationType.EMAIL;

		Notification notification = Notification.create(id, userId, message, type);

		ArgumentCaptor<NotificationOutbox> outboxCaptor = ArgumentCaptor.forClass(NotificationOutbox.class);
		given(outboxRepository.save(any(NotificationOutbox.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

		// when
		notificationSenderAdapter.send(notification);

		// then
		then(outboxRepository).should().save(outboxCaptor.capture());
		NotificationOutbox capturedOutbox = outboxCaptor.getValue();

		assertThat(capturedOutbox.getId()).isEqualTo(id);
		assertThat(capturedOutbox.getUserId()).isEqualTo(userId);
		assertThat(capturedOutbox.getMessage()).isEqualTo(message);
		assertThat(capturedOutbox.getNotificationType()).isEqualTo(type);
	}
}