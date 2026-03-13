package com.notification.relay.application.usecase;

import java.util.List;

import com.notification.relay.application.dto.query.GetHistoryQuery;
import com.notification.relay.application.dto.result.NotificationHistoryResult;

public interface GetNotificationHistoryUseCase {

	List<NotificationHistoryResult> execute(GetHistoryQuery query);
}
