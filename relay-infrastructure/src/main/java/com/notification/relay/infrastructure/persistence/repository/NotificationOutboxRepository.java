package com.notification.relay.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.notification.relay.infrastructure.persistence.entity.NotificationOutbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

	List<NotificationOutbox> findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
			NotificationOutbox.OutBoxStatus status,
			LocalDateTime createdAt,
			Pageable pageable
	);
}
