package com.notification.relay.worker.exception;

public class ExternalApiUnavailableException extends RuntimeException {

	public ExternalApiUnavailableException(String message) {
		super(message);
	}
}
