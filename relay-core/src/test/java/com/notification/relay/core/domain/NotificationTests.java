package com.notification.relay.core.domain;

import com.notification.relay.core.exception.InvalidNotificationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTests {

	@Test
	@DisplayName("정상적인 Notification 생성")
	void create_success() {
		// given
		long id = 123456789L;
		String userId = "user1234";
		String message = "테스트 메시지";
		NotificationType type = NotificationType.SMS;

		// when
		Notification notification = Notification.create(id, userId, message, type);

		// then
		assertThat(notification.getId()).isEqualTo(id);
		assertThat(notification.getUserId()).isEqualTo(userId);
		assertThat(notification.getMessage()).isEqualTo(message);
		assertThat(notification.getNotificationType()).isEqualTo(type);
	}

	@Test
	@DisplayName("ID가 0이면 예외 발생")
	void create_fail_when_id_is_zero() {
		// given
		long id = 0L;
		String userId = "user1234";
		String message = "테스트 메시지";
		NotificationType type = NotificationType.SMS;

		// when & then
		assertThatThrownBy(() -> Notification.create(id, userId, message, type))
				.isInstanceOf(InvalidNotificationException.class)
				.hasMessageContaining("ID cannot be 0");
	}

	@Test
	@DisplayName("userId가 빈 문자열이면 예외 발생")
	void create_fail_when_id_is_blank() {
		// given
		long id = 123456789L;
		String userId = "";
		String message = "테스트 메시지";
		NotificationType type = NotificationType.SMS;

		// when & then
		assertThatThrownBy(() -> Notification.create(id, userId, message, type))
				.isInstanceOf(InvalidNotificationException.class)
				.hasMessageContaining("User ID cannot be null or empty");
	}

	@Test
	@DisplayName("userId가 null이면 예외 발생")
	void create_fail_when_message_is_null() {
		// given
		long id = 123456789L;
		String userId = "user123";
		String message = null;
		NotificationType type = NotificationType.SMS;

		// when & then
		assertThatThrownBy(() -> Notification.create(id, userId, message, type))
				.isInstanceOf(InvalidNotificationException.class)
				.hasMessageContaining("Message cannot be null or empty");
	}

	@Test
	@DisplayName("userId가 null이면 예외 발생")
	void create_fail_when_message_exceeds_1000_characters() {
		// given
		long id = 123456789L;
		String userId = "user123";
		String message = "a".repeat(1001); // 1001자 메시지
		NotificationType type = NotificationType.SMS;

		// when & then
		assertThatThrownBy(() -> Notification.create(id, userId, message, type))
				.isInstanceOf(InvalidNotificationException.class)
				.hasMessageContaining("Message cannot exceed 1000 characters");
	}
}