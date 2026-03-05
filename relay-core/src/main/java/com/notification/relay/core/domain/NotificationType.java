package com.notification.relay.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

	SMS("SMS"),
	KAKAO("KAKAO"),
	EMAIL("EMAIL"),
	;
	private final String description;
}
