package nars.nal.meta;

import nars.Memory;
import nars.Premise;
import nars.task.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public interface TruthOperator {

    /**
     *
     * @param task
     * @param belief
     * @param m
     * @param minConf if confidence is less than minConf, it can return null without creating the Truth instance;
     *                if confidence is equal to or greater, then it is valid
     * @return
     */
    @Nullable Truth apply(@Nullable Truth task, @Nullable Truth belief, @NotNull Memory m, float minConf);


    boolean allowOverlap();
    boolean single();
}
