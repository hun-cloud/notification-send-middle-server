package com.notification.relay.worker.idempotency;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisIdempotencyChecker implements IdempotencyChecker {

	private final RedisTemplate<String, String> redisTemplate;
	private static final Duration TTL = Duration.ofDays(1);
	private static final String PREFIX = "processed:notification:";

	public boolean isAlreadyProcessed(String notificationId) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + notificationId));
	}

	public void markAsProcessed(String notificationId) {
		redisTemplate.opsForValue().set(PREFIX + notificationId, "1", TTL);
	}
}
