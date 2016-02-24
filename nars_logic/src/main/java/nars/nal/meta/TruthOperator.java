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

    default boolean apply(@NotNull PremiseEval m) {

        Premise premise = m.currentPremise;


        @Nullable Task belief = premise.belief();

        float minConf = m.getMinConfidence();

        Truth truth = apply(
                premise.task().truth(),
                (belief == null) ? null : belief.truth(),
                premise.memory(),
                minConf
        );

        //pre-filter insufficient confidence level

        if ((truth!=null) && (truth.conf() > minConf)) {
            m.truth.set(truth);
            return true;
        }
        return false;
    }

    boolean allowOverlap();
    boolean single();
}
