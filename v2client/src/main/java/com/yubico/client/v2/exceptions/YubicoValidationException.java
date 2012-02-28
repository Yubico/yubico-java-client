package com.yubico.client.v2.exceptions;

public class YubicoValidationException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public YubicoValidationException(String message, Throwable cause) {
		super(message, cause);
	}

	public YubicoValidationException(String message) {
		super(message);
	}
}
