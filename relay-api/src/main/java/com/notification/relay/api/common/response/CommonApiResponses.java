package com.notification.relay.api.common.response;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
		@ApiResponse(responseCode = "200", description = "요청 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 (클라이언트 오류)"),
		@ApiResponse(responseCode = "422", description = "유효하지 않은 데이터 (구문 오류 포함)"),
		@ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public @interface CommonApiResponses {
}
