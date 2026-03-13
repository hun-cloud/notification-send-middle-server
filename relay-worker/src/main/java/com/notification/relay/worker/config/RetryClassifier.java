package com.notification.relay.worker.config;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;

import javax.net.ssl.SSLException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@Component
public class RetryClassifier {

	public boolean isRetryable(Throwable throwable) {
		if (throwable instanceof HttpStatusCodeException httpEx) {
			return isRetryableStatusCode(httpEx);
		}

		if (throwable instanceof ResourceAccessException resourceEx) {
			return isRetryableNetworkError(resourceEx.getCause());
		}

		return false;
	}

	private boolean isRetryableStatusCode(HttpStatusCodeException httpEx) {
		int statusCode = httpEx.getStatusCode().value();

		// 429 Too Many Requests: 일시적 요청 제한 → 재시도
		if (statusCode == 429) {
			log.warn("[RetryClassifier] HTTP 429 Too Many Requests → retryable");
			return true;
		}

		// 4xx 클라이언트 오류: 요청 자체의 문제 → 재시도 무의미
		if (httpEx instanceof HttpClientErrorException) {
			log.debug("[RetryClassifier] HTTP {} Client Error → non-retryable", statusCode);
			return false;
		}

		// 5xx 서버 오류 중 501은 재시도 무의미
		if (statusCode == 501) {
			log.debug("[RetryClassifier] HTTP 501 Not Implemented → non-retryable");
			return false;
		}

		// 나머지 5xx (500, 502, 503, 504 등): 일시적 서버 장애 → 재시도
		if (httpEx instanceof HttpServerErrorException) {
			log.warn("[RetryClassifier] HTTP {} Server Error → retryable", statusCode);
			return true;
		}

		log.debug("[RetryClassifier] HTTP {} → non-retryable (default)", statusCode);
		return false;
	}

	private boolean isRetryableNetworkError(Throwable cause) {
		if (cause == null) {
			log.debug("[RetryClassifier] ResourceAccessException (cause=null) → retryable (default)");
			return true;
		}

		// Retryable: 읽기/연결 타임아웃 → 서버가 응답을 늦게 주는 일시적 상황
		if (cause instanceof SocketTimeoutException) {
			log.warn("[RetryClassifier] SocketTimeoutException → retryable (read/connect timeout)");
			return true;
		}

		// Retryable: 연결 거부/리셋 → 서버가 일시적으로 연결을 받지 못하는 상황
		if (cause instanceof ConnectException) {
			log.warn("[RetryClassifier] ConnectException → retryable (connection refused/reset)");
			return true;
		}

		// Non-Retryable: DNS 실패 → 호스트 자체를 찾을 수 없으므로 즉시 재시도 무의미
		if (cause instanceof UnknownHostException) {
			log.error("[RetryClassifier] UnknownHostException → non-retryable (DNS resolution failure)");
			return false;
		}

		// Non-Retryable: TLS/SSL 오류 → 인증서 문제는 재시도로 해결 불가
		if (cause instanceof SSLException) {
			log.error("[RetryClassifier] SSLException → non-retryable (TLS/certificate error)");
			return false;
		}

		// 알 수 없는 네트워크 오류는 안전한 방향(retryable)으로 처리
		log.warn("[RetryClassifier] Unknown network cause: {} → retryable (default safe)",
				cause.getClass().getSimpleName());
		return true;
	}
}
