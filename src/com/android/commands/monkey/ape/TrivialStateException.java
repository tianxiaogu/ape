package com.android.commands.monkey.ape;

public class TrivialStateException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public TrivialStateException() {
        super();
    }

    public TrivialStateException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public TrivialStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public TrivialStateException(String message) {
        super(message);
    }

    public TrivialStateException(Throwable cause) {
        super(cause);
    }

}
