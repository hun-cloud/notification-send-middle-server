package com.notification.relay.worker.consumer;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.assertj.core.api.Assertions.assertThat;

class MessageParserTest {

	private MessageParser messageParser;

	@BeforeEach
	void setUp() {
		messageParser = new MessageParser();
	}

	@Test
	@DisplayName("notificationId 헤더를 정상적으로 파싱한다")
	void getNotificationId_success() {
		// given
		MessageProperties props = new MessageProperties();
		props.setHeader("notificationId", "noti-123");
		Message message = new Message("body".getBytes(), props);

		// when
		String result = messageParser.getNotificationId(message);

		// then
		assertThat(result).isEqualTo("noti-123");
	}

	@Test
	@DisplayName("notificationId 헤더가 없으면 unknown을 반환한다")
	void getNotificationId_missing_header() {
		// given
		Message message = new Message("body".getBytes(), new MessageProperties());

		// when
		String result = messageParser.getNotificationId(message);

		// then
		assertThat(result).isEqualTo("unknown");
	}

	@Test
	@DisplayName("userId 헤더를 receiver로 정상적으로 파싱한다")
	void getReceiver_success() {
		// given
		MessageProperties props = new MessageProperties();
		props.setHeader("userId", "user-456");
		Message message = new Message("body".getBytes(), props);

		// when
		String result = messageParser.getReceiver(message);

		// then
		assertThat(result).isEqualTo("user-456");
	}

	@Test
	@DisplayName("메시지 바디를 문자열로 파싱한다")
	void getBody_success() {
		// given
		String body = "테스트 메시지";
		Message message = new Message(body.getBytes(), new MessageProperties());

		// when
		String result = messageParser.getBody(message);

		// then
		assertThat(result).isEqualTo(body);
	}

	@Test
	@DisplayName("x-death 헤더가 없으면 deathCount는 0이다")
	void getDeathCount_no_x_death() {
		// given
		Message message = new Message("body".getBytes(), new MessageProperties());

		// when
		int result = messageParser.getDeathCount(message);

		// then
		assertThat(result).isZero();
	}

	@Test
	@DisplayName("x-death 헤더가 있으면 count를 반환한다")
	void getDeathCount_with_x_death() {
		// given
		MessageProperties props = new MessageProperties();
		props.setHeader("x-death", List.of(Map.of("count", 2L)));
		Message message = new Message("body".getBytes(), props);

		// when
		int result = messageParser.getDeathCount(message);

		// then
		assertThat(result).isEqualTo(2);
	}

	@Test
	@DisplayName("x-death 헤더가 빈 리스트면 deathCount는 0이다")
	void getDeathCount_empty_x_death() {
		// given
		MessageProperties props = new MessageProperties();
		props.setHeader("x-death", List.of());
		Message message = new Message("body".getBytes(), props);

		// when
		int result = messageParser.getDeathCount(message);

		// then
		assertThat(result).isZero();
	}
}
