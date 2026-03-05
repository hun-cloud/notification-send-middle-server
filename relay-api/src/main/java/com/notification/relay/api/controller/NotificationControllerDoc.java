package com.notification.relay.api.controller;

import com.notification.relay.api.common.response.CommonApiResponses;
import com.notification.relay.api.common.response.CommonResponse;
import com.notification.relay.api.controller.dto.request.NotificationRegisterRequest;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

@Tag(name = "Notification - 알림", description = "알림 관련 API 모음")
public interface NotificationControllerDoc {

	@CommonApiResponses
	ResponseEntity<CommonResponse<Void>> sendNotification(NotificationRegisterRequest request);
}
