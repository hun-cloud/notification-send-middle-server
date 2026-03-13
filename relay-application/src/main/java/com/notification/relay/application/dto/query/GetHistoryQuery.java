package com.notification.relay.application.dto.query;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GetHistoryQuery {

	private final String userId;
	private final Long cursor;
	private final int size;

	public static GetHistoryQuery of(String userId, Long cursor, int size) {
		return new GetHistoryQuery(userId, cursor, size);
	}
}
