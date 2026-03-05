package com.notification.relay.worker.sender;

public interface NotificationSender {
	void send(SendRequest request);
}
