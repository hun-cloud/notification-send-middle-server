package com.notification.relay.api.controller;

import com.notification.relay.api.controller.dto.request.NotificationRegisterRequest;
import com.notification.relay.application.common.response.CommonResponse;
import com.notification.relay.application.usecase.SendNotificationUseCase;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDoc {

	private final SendNotificationUseCase notificationUseCase;

	// todo 알림 발송 등록
	public ResponseEntity<CommonResponse<Void>> sendNotification(@RequestBody NotificationRegisterRequest request) {

		notificationUseCase.execute(request.toCommand());

		return ResponseEntity.ok(CommonResponse.success());
	}

	// todo 알림 내역 조회
}
