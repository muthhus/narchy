package nars.term;

import nars.Op;
import nars.Param;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static nars.time.Tense.DTERNAL;

/**
 * Created by me on 2/26/16.
 */
public final class InvalidTermException extends RuntimeException {

    @NotNull private final Op op;
    private final int dt;
    @NotNull private final Term[] args;
    @NotNull private final String reason;



    public InvalidTermException(@NotNull Op op, @NotNull Term[] args, @NotNull String reason) {
        this(op, DTERNAL, args, reason);
    }

    public InvalidTermException(@NotNull Op op, int dt, @NotNull Term[] args, @NotNull String reason) {
        this.op = op;
        this.dt = dt;
        this.args = args;
        this.reason = reason;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        if (!Param.DEBUG)
            return this; //omit stacktrace if not in debug mode for efficiency
        return super.fillInStackTrace();
    }

    @NotNull
    @Override
    public String getMessage() {
        return toString();
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + reason + " {" +
                op +
                ", dt=" + dt +
                ", args=" + Arrays.toString(args) +
                '}';
    }
}
