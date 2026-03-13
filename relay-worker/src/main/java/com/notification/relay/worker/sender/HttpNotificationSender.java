package com.notification.relay.worker.sender;

import com.notification.relay.worker.config.SenderProperties;
import com.notification.relay.worker.exception.ExternalApiUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpNotificationSender implements NotificationSender {

	private final RestClient restClient;
	private final SenderProperties senderProperties;

	@CircuitBreaker(name = "notificationSender", fallbackMethod = "fallback")
	@Retry(name = "notificationSender")
	@Override
	public void send(SendRequest request) {
		restClient.post()
				.uri(senderProperties.getUrl() + "/mock/send")
				.body(request)
				.retrieve()
				.toBodilessEntity();

		log.info("[HttpNotificationSender] 발송 완료: requestId={}, channelType={}",
				request.getRequestId(), request.getChannelType());
	}

	private void fallback(SendRequest request, Exception e) {
		log.error("[HttpNotificationSender] Circuit Breaker OPEN - 외부 API 차단 중: channelType={}, cause={}",
				request.getChannelType(), e.getMessage());
		throw new ExternalApiUnavailableException("외부 발송 API 일시 중단: " + e.getMessage());
	}
}
