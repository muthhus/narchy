package nars.task;


import nars.Task;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Tasked  {

    @Nullable Task task();

}
