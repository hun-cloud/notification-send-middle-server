package com.notification.relay.worker.idempotency;

public interface IdempotencyChecker {

	boolean isAlreadyProcessed(String notificationId);

	void markAsProcessed(String notificationId);
}
