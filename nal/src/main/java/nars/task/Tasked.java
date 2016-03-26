package nars.task;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Tasked  {

    @NotNull
    Task task();

    @Nullable
    static Task the(Object v) {
        if (v instanceof Tasked)
            return ((Tasked)v).task();
        return null;
    }
}
