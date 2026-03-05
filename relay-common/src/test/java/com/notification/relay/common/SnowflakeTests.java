package com.notification.relay.common;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SnowflakeTests {

	@Test
	@DisplayName("ID 생성 성공")
	void generate_success() {
		// given
		Snowflake snowflake = new Snowflake();

		// when
		long id = snowflake.generate();

		// then
		assertThat(id).isNotZero();
	}

	@Test
	@DisplayName("생성된 ID는 매번 다름")
	void generate_unique_ids() {
		// given
		Snowflake snowflake = new Snowflake();

		// when
		long id1 = snowflake.generate();
		long id2 = snowflake.generate();
		long id3 = snowflake.generate();

		// then
		assertThat(id1).isNotEqualTo(id2);
		assertThat(id2).isNotEqualTo(id3);
		assertThat(id1).isNotEqualTo(id3);
	}

	@Test
	@DisplayName("10000개 ID 생성 시 중복 없음")
	void generate_10000_unique_ids() {
		// given
		Snowflake snowflake = new Snowflake();
		Set<Long> ids = new HashSet<>();
		int count = 10_000;

		// when
		for (int i = 0; i < count; i++) {
			ids.add(snowflake.generate());
		}

		// then
		assertThat(ids).hasSize(count);
	}

	@Test
	@DisplayName("멀티스레드 환경에서 ID 중복 없음")
	void generate_unique_ids_in_multithreaded_environment() throws InterruptedException {
		// given
		Snowflake snowflake = new Snowflake();
		int threadCount = 10;
		int idsPerThread = 1000;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		Set<Long> ids = new HashSet<>();

		// when
		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				for (int j = 0; j < idsPerThread; j++) {
					synchronized (ids) {
						ids.add(snowflake.generate());
					}
				}
				latch.countDown();
			});
		}

		latch.await();
		executorService.shutdown();

		// then
		assertThat(ids).hasSize(threadCount * idsPerThread);
	}
}