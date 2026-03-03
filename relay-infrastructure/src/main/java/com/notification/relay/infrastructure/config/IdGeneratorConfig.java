package com.notification.relay.infrastructure.config;

import com.notification.relay.common.IdGenerator;
import com.notification.relay.common.Snowflake;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class IdGeneratorConfig {

	private final SnowflakeProperties snowflakeProperties;

	@Bean
	public IdGenerator idGenerator() {

		long workerId = snowflakeProperties.getWorkerId();
		long dataCenterId = snowflakeProperties.getDataCenterId();

		if (workerId < 0 || workerId > 31) {
			throw new IllegalArgumentException("Worker ID must be between 0 and 31");
		}

		if (dataCenterId < 0 || dataCenterId > 31) {
			throw new IllegalArgumentException("Data Center ID must be between 0 and 31");
		}

		return new Snowflake(workerId, dataCenterId);
	}
}
