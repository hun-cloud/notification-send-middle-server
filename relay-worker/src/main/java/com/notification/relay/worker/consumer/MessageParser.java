package com.notification.relay.worker.consumer;

import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageParser {

	public String getNotificationId(Message message) {
		return getHeader(message, "notificationId");
	}

	public String getReceiver(Message message) {
		return getHeader(message, "userId");
	}

	public String getBody(Message message) {
		return new String(message.getBody());
	}

	public int getDeathCount(Message message) {
		List<?> xDeath = message.getMessageProperties().getHeader("x-death");
		if (xDeath == null || xDeath.isEmpty()) return 0;
		Map<?, ?> deathInfo = (Map<?, ?>) xDeath.get(0);
		Number count = (Number) deathInfo.get("count");
		return count != null ? count.intValue() : 0;
	}

	private String getHeader(Message message, String key) {
		Object value = message.getMessageProperties().getHeader(key);
		return value != null ? String.valueOf(value) : "unknown";
	}
}
