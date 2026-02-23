package com.notification.relay.infrastructure.config;

import com.notification.relay.common.IdGenerator;
import com.notification.relay.common.Snowflake;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdGeneratorConfig {

	@Bean
	public IdGenerator idGenerator() {
		return new Snowflake();
	}
}
