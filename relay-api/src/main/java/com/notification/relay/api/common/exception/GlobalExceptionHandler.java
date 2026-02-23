package com.notification.relay.api.common.exception;

import com.notification.relay.api.common.response.ErrorResponse;
import com.notification.relay.core.exception.InvalidNotificationException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(InvalidNotificationException.class)
	public ResponseEntity<ErrorResponse> handleInvalidNotificationException(InvalidNotificationException e) {

		log.error("InvalidNotificationException: {}", e.getMessage());

		ErrorResponse errorResponse = ErrorResponse.of(400, e.getMessage());

		return ResponseEntity.badRequest().body(errorResponse);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {

		log.error("ValidationException: {}", e.getMessage());

		String errorMessage = e.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.findFirst()
				.orElse("Validation error");
		ErrorResponse errorResponse = ErrorResponse.of(400, errorMessage);

		return ResponseEntity.badRequest().body(errorResponse);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {

		log.error("IllegalArgumentException: {}", e.getMessage());

		ErrorResponse errorResponse = ErrorResponse.of(400, e.getMessage());

		return ResponseEntity.badRequest().body(errorResponse);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {

		log.error("Unexpected error: {}", e.getMessage(), e);

		ErrorResponse errorResponse = ErrorResponse.of(500, "An unexpected error occurred");

		return ResponseEntity.status(500).body(errorResponse);
	}
}
