package com.notification.relay.api.controller;

import com.notification.relay.api.common.response.CommonResponse;
import com.notification.relay.api.controller.dto.request.NotificationRegisterRequest;
import com.notification.relay.application.usecase.SendNotificationUseCase;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDoc {

	private final SendNotificationUseCase notificationUseCase;

	@PostMapping
	public ResponseEntity<CommonResponse<Void>> sendNotification(@RequestBody NotificationRegisterRequest request) {

		notificationUseCase.execute(request.toCommand());

		return ResponseEntity.ok(CommonResponse.success());
	}

	// todo 알림 내역 조회
}
