package com.notification.relay.application.service;

import com.notification.relay.application.dto.command.SendNotificationCommand;
import com.notification.relay.application.port.out.NotificationSenderPort;
import com.notification.relay.common.IdGenerator;
import com.notification.relay.core.domain.Notification;
import com.notification.relay.core.domain.NotificationType;
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
class SendNotificationServiceTests {

	@Mock
	private NotificationSenderPort notificationSenderPort;

	@Mock
	private IdGenerator idGenerator;

	@InjectMocks
	private SendNotificationService sendNotificationService;

	@Test
	@DisplayName("알림 전송 성공")
	void execute_success() {
	    //given
		String userId = "user123";
		String message = "테스트 메시지";
		NotificationType type = NotificationType.SMS;
		long generatedId = 123456789L;

		SendNotificationCommand command = SendNotificationCommand.of(userId, message, type);
		given(idGenerator.generate()).willReturn(generatedId);

		//when
		sendNotificationService.execute(command);

		//then
		then(idGenerator).should(times(1)).generate();
		then(notificationSenderPort).should(times(1)).send(any(Notification.class));
	}

	@Test
	@DisplayName("생성된 ID로 Notification 생성 후 전송")
	void execute_creates_notification_with_generated_id() {
		// given
		String userId = "user123";
		String message = "테스트 메시지";
		NotificationType type = NotificationType.SMS;
		long generatedId = 123456789L;

		SendNotificationCommand command = SendNotificationCommand.of(userId, message, type);
		given(idGenerator.generate()).willReturn(generatedId);

		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

		// when
		sendNotificationService.execute(command);

		// then
		then(notificationSenderPort).should().send(notificationCaptor.capture());
		Notification capturedNotification = notificationCaptor.getValue();

		assertThat(capturedNotification.getId()).isEqualTo(generatedId);
		assertThat(capturedNotification.getUserId()).isEqualTo(userId);
		assertThat(capturedNotification.getMessage()).isEqualTo(message);
		assertThat(capturedNotification.getNotificationType()).isEqualTo(type);
	}
}