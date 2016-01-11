package nars.task;


@FunctionalInterface
public interface Tasked {

    Task getTask();

    static Task the(Object v) {
        if (v instanceof Tasked)
            return ((Tasked)v).getTask();
        return null;
    }
}
