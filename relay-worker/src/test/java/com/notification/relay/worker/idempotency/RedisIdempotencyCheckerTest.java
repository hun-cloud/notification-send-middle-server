package com.notification.relay.worker.idempotency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class RedisIdempotencyCheckerTest {

	@Mock
	private RedisTemplate<String, String> redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private RedisIdempotencyChecker checker;

	private static final String PREFIX = "processed:notification:";

	@Test
	@DisplayName("Redis에 키가 존재하면 이미 처리된 메시지로 판단한다")
	void isAlreadyProcessed_key_exists() {
		// given
		String notificationId = "noti-123";
		given(redisTemplate.hasKey(PREFIX + notificationId)).willReturn(true);

		// when
		boolean result = checker.isAlreadyProcessed(notificationId);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("Redis에 키가 없으면 처리되지 않은 메시지로 판단한다")
	void isAlreadyProcessed_key_not_exists() {
		// given
		String notificationId = "noti-123";
		given(redisTemplate.hasKey(PREFIX + notificationId)).willReturn(false);

		// when
		boolean result = checker.isAlreadyProcessed(notificationId);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("Redis 응답이 null이면 처리되지 않은 메시지로 판단한다")
	void isAlreadyProcessed_null_response() {
		// given
		String notificationId = "noti-123";
		given(redisTemplate.hasKey(PREFIX + notificationId)).willReturn(null);

		// when
		boolean result = checker.isAlreadyProcessed(notificationId);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("처리 완료 후 Redis에 1일 TTL로 키를 저장한다")
	void markAsProcessed_saves_key_with_ttl() {
		// given
		String notificationId = "noti-123";
		given(redisTemplate.opsForValue()).willReturn(valueOperations);

		// when
		checker.markAsProcessed(notificationId);

		// then
		then(valueOperations).should(times(1))
				.set(PREFIX + notificationId, "1", Duration.ofDays(1));
	}
}
