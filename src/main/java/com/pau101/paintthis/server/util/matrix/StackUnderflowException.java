package com.pau101.paintthis.server.util.matrix;

public final class StackUnderflowException extends RuntimeException {
	private static final long serialVersionUID = -6946629885006358454L;

	public StackUnderflowException() {
		super();
	}

	public StackUnderflowException(String s) {
		super(s);
	}
}
