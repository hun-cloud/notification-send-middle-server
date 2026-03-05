package com.notification.relay.application.port.out;

import com.notification.relay.core.domain.Notification;

public interface NotificationSenderPort {
	void send(Notification newNotification);
}