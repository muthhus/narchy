package nars.task.util;

import nars.Param;
import nars.Task;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 10/15/16.
 */
public final class InvalidTaskException extends RuntimeException {

    public final Termed task;


    public InvalidTaskException(Termed t, String message) {
        super(message);
        this.task = t;
        if (t instanceof Task)
            ((Task) t).delete(message);
    }

    @NotNull
    @Override
    public String getMessage() {
        return super.getMessage() + ": " + task.toString();
//        return super.getMessage() + ": " +
//                ((task instanceof Task) ? ((Task) task).proof() : task.toString());
    }

    @Override
    public Throwable fillInStackTrace() {
        if (!Param.DEBUG)
            return this; //omit stacktrace if not in debug mode for efficiency
        return super.fillInStackTrace();
    }

}
