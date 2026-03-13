package com.notification.relay.worker.sender;

import com.notification.relay.worker.config.SenderProperties;
import com.notification.relay.worker.exception.ExternalApiUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class HttpNotificationSenderTest {

	@Mock
	private RestClient restClient;

	@Mock
	private SenderProperties senderProperties;

	// 플루언트 API는 RETURNS_SELF로 uri(), body() stubbing 불필요
	@Mock(answer = Answers.RETURNS_SELF)
	private RestClient.RequestBodyUriSpec requestBodyUriSpec;

	@Mock
	private RestClient.ResponseSpec responseSpec;

	@InjectMocks
	private HttpNotificationSender sender;

	private SendRequest sendRequest;

	@BeforeEach
	void setUp() {
		sendRequest = new SendRequest("noti-123", "SMS", "user-456", "테스트 메시지");
	}

	private void givenRestClientChain() {
		given(senderProperties.getUrl()).willReturn("http://localhost:8081");
		given(restClient.post()).willReturn(requestBodyUriSpec);
		given(requestBodyUriSpec.retrieve()).willReturn(responseSpec);
	}

	@Test
	@DisplayName("외부 API 호출 성공 시 정상 완료된다")
	void send_success() {
		// given
		givenRestClientChain();
		given(responseSpec.toBodilessEntity()).willReturn(ResponseEntity.ok().build());

		// when
		sender.send(sendRequest);

		// then
		then(restClient).should(times(1)).post();
	}

	@Test
	@DisplayName("외부 API 호출 실패 시 예외가 발생한다")
	void send_failure() {
		// given
		givenRestClientChain();
		given(responseSpec.toBodilessEntity())
				.willThrow(new ResourceAccessException("연결 실패"));

		// when & then
		assertThatThrownBy(() -> sender.send(sendRequest))
				.isInstanceOf(ResourceAccessException.class);
	}

	@Test
	@DisplayName("Circuit Breaker fallback 호출 시 ExternalApiUnavailableException을 던진다")
	void fallback_throws_external_api_unavailable_exception() throws Exception {
		// given
		Exception cause = new ResourceAccessException("외부 API 장애");

		// when & then
		var fallbackMethod = HttpNotificationSender.class
				.getDeclaredMethod("fallback", SendRequest.class, Exception.class);
		fallbackMethod.setAccessible(true);

		assertThatThrownBy(() -> fallbackMethod.invoke(sender, sendRequest, cause))
				.getCause()
				.isInstanceOf(ExternalApiUnavailableException.class)
				.hasMessageContaining("외부 발송 API 일시 중단");
	}
}
