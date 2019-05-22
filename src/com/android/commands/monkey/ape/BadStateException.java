package com.android.commands.monkey.ape;

public class BadStateException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public BadStateException() {
        super();
    }

    public BadStateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public BadStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadStateException(String message) {
        super(message);
    }

    public BadStateException(Throwable cause) {
        super(cause);
    }

}
