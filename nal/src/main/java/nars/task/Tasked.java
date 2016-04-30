package nars.task;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Tasked  {

    @NotNull
    Task task();

    @Nullable
    static Task the(Object possiblyTask) {
        if (possiblyTask instanceof Tasked)
            return ((Tasked)possiblyTask).task();
        return null;
    }
}
