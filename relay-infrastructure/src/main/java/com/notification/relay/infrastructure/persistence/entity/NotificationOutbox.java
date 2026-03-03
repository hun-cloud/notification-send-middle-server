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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
		name = "notification_outbox",
		indexes = {
				@Index(name = "idx_status_created_at", columnList = "status, createdAt"),
				@Index(name = "idx_user_id_created_at", columnList = "userId, createdAt")
		}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationOutbox {

	@Id
	private Long id;

	@Column(nullable = false)
	private String userId;

	@Column(nullable = false, length = 1000)
	private String message;

	@Enumerated(EnumType.STRING)
	private NotificationType notificationType;

	@Enumerated(EnumType.STRING)
	private OutBoxStatus status;

	private LocalDateTime createdAt;
	private LocalDateTime publishedAt;

	@Version
	private Long version;

	@Column(nullable = false)
	private Integer retryCount = 0;

	@Column(length = 500)
	private String errorMessage;

	public enum OutBoxStatus {
		PENDING,
		PUBLISHED,
		FAILED
	}

	public static NotificationOutbox create(Long id, String userId, String message, NotificationType notificationType) {
		NotificationOutbox outbox = new NotificationOutbox();
		outbox.id = id;
		outbox.userId = userId;
		outbox.message = message;
		outbox.notificationType = notificationType;
		outbox.status = OutBoxStatus.PENDING;
		outbox.createdAt = LocalDateTime.now();
		outbox.retryCount = 0;
		return outbox;
	}

	public void markAsPublished() {
		this.status = OutBoxStatus.PUBLISHED;
		this.publishedAt = LocalDateTime.now();
	}

	public void markAsFailed(String errorMessage) {
		this.status = OutBoxStatus.FAILED;
		this.errorMessage = errorMessage;
		this.retryCount++;
	}

	public boolean canRetry(int maxRetryCount) {
		return retryCount < maxRetryCount;
	}

	public void incrementRetry(String message) {
		this.retryCount++;
		this.errorMessage = message;
	}
}
