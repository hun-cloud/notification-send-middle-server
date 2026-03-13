package com.notification.relay.worker.sender;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SendRequest {
	private final String requestId;
	private final String channelType;
	private final String receiver;
	private final String message;
}
