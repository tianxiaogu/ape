package com.android.commands.monkey.ape;

public class NoValidActionException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public NoValidActionException() {
        super();
    }

    public NoValidActionException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public NoValidActionException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoValidActionException(String message) {
        super(message);
    }

    public NoValidActionException(Throwable cause) {
        super(cause);
    }

}
