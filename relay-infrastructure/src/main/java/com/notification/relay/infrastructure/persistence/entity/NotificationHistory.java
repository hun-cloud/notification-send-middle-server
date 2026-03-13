package com.notification.relay.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import com.notification.relay.core.domain.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
		name = "notification_history",
		indexes = {
				@Index(name = "idx_history_user_id", columnList = "userId, createdAt DESC, id DESC")
		}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationHistory {

	@Id
	private Long id;

	@Column(nullable = false)
	private String userId;

	@Column(nullable = false, length = 1000)
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationType notificationType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SendStatus sendStatus;

	@Column(length = 500)
	private String errorMessage;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	public enum SendStatus {
		SUCCESS,
		FAILED
	}

	public static NotificationHistory success(Long id, String userId, String message, NotificationType notificationType) {
		NotificationHistory history = new NotificationHistory();
		history.id = id;
		history.userId = userId;
		history.message = message;
		history.notificationType = notificationType;
		history.sendStatus = SendStatus.SUCCESS;
		history.createdAt = LocalDateTime.now();
		return history;
	}

	public static NotificationHistory failed(Long id, String userId, String message, NotificationType notificationType, String errorMessage) {
		NotificationHistory history = new NotificationHistory();
		history.id = id;
		history.userId = userId;
		history.message = message;
		history.notificationType = notificationType;
		history.sendStatus = SendStatus.FAILED;
		history.errorMessage = errorMessage;
		history.createdAt = LocalDateTime.now();
		return history;
	}
}
