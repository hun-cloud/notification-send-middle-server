package com.notification.relay.common;

import cn.hutool.core.util.IdUtil;

public class Snowflake implements IdGenerator {

	private final cn.hutool.core.lang.Snowflake snowflake;

	public Snowflake(long workerId, long datacenterId) {
		this.snowflake = IdUtil.getSnowflake(workerId, datacenterId);
	}

	@Override
	public long generate() {
		return snowflake.nextId();
	}
}
