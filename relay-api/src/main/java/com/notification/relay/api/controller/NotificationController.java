package com.notification.relay.api.controller;

import java.util.List;

import com.notification.relay.api.common.response.CommonResponse;
import com.notification.relay.api.controller.dto.request.NotificationRegisterRequest;
import com.notification.relay.application.dto.query.GetHistoryQuery;
import com.notification.relay.application.dto.result.NotificationHistoryResult;
import com.notification.relay.application.usecase.GetNotificationHistoryUseCase;
import com.notification.relay.application.usecase.SendNotificationUseCase;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDoc {

	private final SendNotificationUseCase notificationUseCase;
	private final GetNotificationHistoryUseCase getNotificationHistoryUseCase;

	@PostMapping
	public ResponseEntity<CommonResponse<Void>> sendNotification(@RequestBody NotificationRegisterRequest request) {

		notificationUseCase.execute(request.toCommand());

		return ResponseEntity.ok(CommonResponse.success());
	}

	@GetMapping("/history")
	public ResponseEntity<CommonResponse<List<NotificationHistoryResult>>> getHistory(
			@RequestParam String userId,
			@RequestParam(defaultValue = "9223372036854775807") Long cursor,
			@RequestParam(defaultValue = "20") int size
	) {
		GetHistoryQuery query = GetHistoryQuery.of(userId, cursor, size);
		List<NotificationHistoryResult> results = getNotificationHistoryUseCase.execute(query);

		return ResponseEntity.ok(CommonResponse.success(results));
	}
}
