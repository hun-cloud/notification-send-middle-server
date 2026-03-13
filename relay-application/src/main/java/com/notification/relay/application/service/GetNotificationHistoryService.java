package com.notification.relay.application.service;

import java.util.List;

import com.notification.relay.application.dto.query.GetHistoryQuery;
import com.notification.relay.application.dto.result.NotificationHistoryResult;
import com.notification.relay.application.port.out.NotificationHistoryQueryPort;
import com.notification.relay.application.usecase.GetNotificationHistoryUseCase;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetNotificationHistoryService implements GetNotificationHistoryUseCase {

	private final NotificationHistoryQueryPort notificationHistoryQueryPort;

	@Override
	public List<NotificationHistoryResult> execute(GetHistoryQuery query) {
		return notificationHistoryQueryPort.findByUserId(
				query.getUserId(),
				query.getCursor(),
				query.getSize()
		);
	}
}
