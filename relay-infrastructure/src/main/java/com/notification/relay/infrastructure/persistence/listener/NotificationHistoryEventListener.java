package com.notification.relay.infrastructure.persistence.listener;

import com.notification.relay.core.event.NotificationFailedEvent;
import com.notification.relay.core.event.NotificationSentEvent;
import com.notification.relay.infrastructure.persistence.entity.NotificationHistory;
import com.notification.relay.infrastructure.persistence.repository.NotificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationHistoryEventListener {

	private final NotificationHistoryRepository notificationHistoryRepository;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handleSentEvent(NotificationSentEvent event) {
		try {
			NotificationHistory history = NotificationHistory.success(
					Long.parseLong(event.getNotificationId()),
					event.getUserId(),
					event.getMessage(),
					event.getNotificationType()
			);
			notificationHistoryRepository.save(history);
			log.info("[History] 발송 성공 저장: notificationId={}", event.getNotificationId());
		} catch (Exception e) {
			log.error("[History] 발송 성공 저장 실패: notificationId={}", event.getNotificationId(), e);
		}
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handleFailedEvent(NotificationFailedEvent event) {
		try {
			NotificationHistory history = NotificationHistory.failed(
					Long.parseLong(event.getNotificationId()),
					event.getUserId(),
					event.getMessage(),
					event.getNotificationType(),
					event.getErrorMessage()
			);
			notificationHistoryRepository.save(history);
			log.info("[History] 발송 실패 저장: notificationId={}", event.getNotificationId());
		} catch (Exception e) {
			log.error("[History] 발송 실패 저장 실패: notificationId={}", event.getNotificationId(), e);
		}
	}
}
