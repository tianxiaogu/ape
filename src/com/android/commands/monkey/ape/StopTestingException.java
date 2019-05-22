package com.android.commands.monkey.ape;

public class StopTestingException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -7469168351124036091L;

    public StopTestingException() {
        super();
    }

    public StopTestingException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public StopTestingException(String message, Throwable cause) {
        super(message, cause);
    }

    public StopTestingException(String message) {
        super(message);
    }

    public StopTestingException(Throwable cause) {
        super(cause);
    }

}
