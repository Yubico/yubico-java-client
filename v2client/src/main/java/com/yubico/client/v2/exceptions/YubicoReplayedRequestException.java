package com.yubico.client.v2.exceptions;

public class YubicoReplayedRequestException extends Exception {
	private static final long serialVersionUID = 1L;

	public YubicoReplayedRequestException(String message) {
		super(message);
	}
}
