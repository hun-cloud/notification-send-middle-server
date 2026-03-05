package com.notification.relay.api.common.response;

import lombok.Builder;
import lombok.Getter;

@Getter
public class CommonResponse<T> {

	private final int code;
	private final boolean result;
	private final String message;
	private final T data;

	@Builder
	private CommonResponse(int code, boolean result, String message, T data) {
		this.code = code;
		this.result = result;
		this.message = message;
		this.data = data;
	}

	public static <T> CommonResponse success(T data) {
		return CommonResponse.builder()
				.code(200)
				.result(true)
				.message("성공")
				.data(data)
				.build();
	}

	public static CommonResponse success() {
		return CommonResponse.builder()
				.code(200)
				.result(true)
				.message("성공")
				.data(null)
				.build();
	}
}
