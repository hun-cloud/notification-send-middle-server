package com.notification.relay.infrastructure.config;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "snowflake")
public class SnowflakeProperties {
	private long workerId = 0L;
	private long dataCenterId = 0L;
}
