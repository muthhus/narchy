package nars.util;

import nars.Param;

/**
 * Created by me on 10/30/16.
 */
public class SoftException extends RuntimeException {

    public SoftException() {
        super();
    }

    public SoftException(String message) {
        super(message);
    }

    @Override
    public final Throwable fillInStackTrace() {
        if (!Param.DEBUG)
            return this; //omit stacktrace if not in debug mode for efficiency
        return super.fillInStackTrace();
    }
}
