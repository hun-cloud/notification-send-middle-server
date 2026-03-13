package com.notification.relay.application.port.out;

import java.util.List;

import com.notification.relay.application.dto.result.NotificationHistoryResult;

public interface NotificationHistoryQueryPort {

	List<NotificationHistoryResult> findByUserId(String userId, Long cursor, int size);
}
