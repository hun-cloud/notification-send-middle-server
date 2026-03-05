package com.notification.relay.api.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {
	private final int code;
	private final boolean result;
	private final String message;

	public static ErrorResponse of(int code, String message) {
		return new ErrorResponse(code, false, message);
	}
}
