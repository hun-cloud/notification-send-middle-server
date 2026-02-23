package com.notification.relay.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.notification.relay")
public class NotificationRelayApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationRelayApplication.class);
	}
}
