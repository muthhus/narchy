package nars.nal.meta;

import nars.Memory;
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


    public static TruthOperator inverse(TruthOperator o) {
        return new InverseTruthOperator(o);
    }

    final class InverseTruthOperator implements TruthOperator {

        private final TruthOperator o;

        public InverseTruthOperator(TruthOperator o) {
            this.o = o;
        }

        @Override
        public
        @Nullable
        Truth apply(@Nullable Truth task, @Nullable Truth belief, @NotNull Memory m, float minConf) {
            return o.apply(belief, task, m, minConf);
        }

        @Override
        public boolean allowOverlap() {
            return o.allowOverlap();
        }

        @Override
        public boolean single() {
            return o.single();
        }
    }
}
