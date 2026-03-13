package com.notification.relay.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.notification.relay.infrastructure.persistence.entity.NotificationHistory;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

	List<NotificationHistory> findByUserIdAndCreatedAtAfterAndIdLessThanOrderByIdDesc(
			String userId,
			LocalDateTime createdAt,
			Long id,
			Pageable pageable
	);
}
