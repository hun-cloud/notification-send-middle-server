package com.notification.relay.infrastructure.adapter.out;

import java.time.LocalDateTime;
import java.util.List;

import com.notification.relay.application.dto.result.NotificationHistoryResult;
import com.notification.relay.application.port.out.NotificationHistoryQueryPort;
import com.notification.relay.infrastructure.persistence.entity.NotificationHistory;
import com.notification.relay.infrastructure.persistence.repository.NotificationHistoryRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationHistoryQueryAdapter implements NotificationHistoryQueryPort {

	private final NotificationHistoryRepository notificationHistoryRepository;

	@Override
	public List<NotificationHistoryResult> findByUserId(String userId, Long cursor, int size) {
		LocalDateTime since = LocalDateTime.now().minusDays(7);

		List<NotificationHistory> histories = notificationHistoryRepository
				.findByUserIdAndCreatedAtAfterAndIdLessThanOrderByIdDesc(
						userId, since, cursor, PageRequest.of(0, size)
				);

		return histories.stream()
				.map(h -> new NotificationHistoryResult(
						h.getId(),
						h.getUserId(),
						h.getMessage(),
						h.getNotificationType(),
						h.getSendStatus().name(),
						h.getErrorMessage(),
						h.getCreatedAt()
				))
				.toList();
	}
}
