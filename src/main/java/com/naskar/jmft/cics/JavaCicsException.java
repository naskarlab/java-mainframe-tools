package com.naskar.jmft.cics;

public class JavaCicsException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public JavaCicsException(String message, Throwable cause) {
		super(message, cause);
	}

	public JavaCicsException(Exception cause) {
		super(cause);
	}

	public JavaCicsException(String message) {
		super(message);
	}

}
