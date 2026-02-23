package com.notification.relay.core.domain;

import com.notification.relay.core.exception.InvalidNotificationException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Notification {
	private final long id;
	private final String userId;
	private final String message;
	private final NotificationType notificationType;

	public static Notification create(long id, String userId, String message, NotificationType notificationType) {
		validateId(id);
		validateUserId(userId);
		validateMessage(message);
		return new Notification(id, userId, message, notificationType);
	}

	private static void validateId(long id) {
		if (id == 0) {
			throw new InvalidNotificationException("ID cannot be 0");
		}
	}

	private static void validateUserId(String userId) {
		if (userId == null || userId.isBlank()) {
			throw new InvalidNotificationException("User ID cannot be null or empty");
		}
	}

	private static void validateMessage(String message) {
		if (message == null || message.isBlank()) {
			throw new InvalidNotificationException("Message cannot be null or empty");
		}

		if (message.length() > 1000) {
			throw new InvalidNotificationException("Message cannot exceed 1000 characters");
		}
	}
}
