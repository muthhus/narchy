package nars.task;


import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Tasked  {

    @Nullable Task task();

}
