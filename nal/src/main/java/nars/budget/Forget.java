package nars.budget;

import nars.NAR;
import nars.bag.BLink;
import nars.nal.Tense;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;


/**
 * Utility methods for Attention Forgetting processes
 */
public enum Forget { ;

    /** acts as a filter to decide if an element should remain in a bag, otherwise some forgetting modification an be applied to a retained item */
    public interface BudgetForgetFilter<X> extends Predicate<BLink<? extends X>>, BudgetForget<X> {
        /** called each frame to update parameters */
        void update(@NotNull NAR nar);
    }

    /** for BLinked budgeted items: if that item becomes Deleted, then the enclosing BLink is removed during a Bag.filter operation that applies this Predicate */
    public static final class ForgetAndDetectDeletion<X extends Budgeted> implements BudgetForgetFilter<X> {

        final BudgetForget<X> forget;

        public ForgetAndDetectDeletion(BudgetForget<X> forget) {
            this.forget = forget;
        }

        @Override
        public boolean test(@NotNull BLink<? extends X> b) {
            //assert(!b.isDeleted());
            if (b.get().isDeleted()) {
                b.delete();
                return false;
            }
            forget.accept(b);
            return true;
        }

        @Override
        public void accept(BLink<? extends X> bLink) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final void update(@NotNull NAR nar) {
            forget.update(nar);
        }

        @Override
        public final void cycle(float subFrame) {
            forget.cycle(subFrame);
        }

    }

    public abstract static class AbstractForget<X> implements BudgetForget<X> {

        @NotNull
        protected final MutableFloat forgetDurations;
        @NotNull
        protected final MutableFloat perfection;

        //cached values for fast repeated accesses

        /** cached value of # cycles equivalent of the supplied forget durations parameter */
        protected transient float forgetCyclesCached = Float.NaN;
        protected transient float perfectionCached = Float.NaN;
        protected transient float now = Float.NaN;
        protected transient float subFrame = Float.NaN;
        private long frame = Tense.TIMELESS;

        public AbstractForget(@NotNull MutableFloat forgetDurations, @NotNull MutableFloat perfection) {
            this.forgetDurations = forgetDurations;
            this.perfection = perfection;
        }

        @Override public abstract void accept(@NotNull BLink<? extends X> budget);

        @Override public void update(@NotNull NAR nar) {
            //same for duration of the cycle
            forgetCyclesCached = forgetDurations.floatValue();
            perfectionCached = perfection.floatValue();
            this.now = frame = nar.time();
        }

        @Override public void cycle(float subFrame) {
            this.now = (this.subFrame = subFrame) + frame;
        }

        @NotNull
        public <B extends Budgeted> ForgetAndDetectDeletion<B> withDeletedItemFiltering() {
            return new ForgetAndDetectDeletion<>((BudgetForget<B>) this);
        }

    }


    /** linaer decay in proportion to time since last forget */
    public static class LinearForget<X> extends AbstractForget<X> {

        @NotNull
        private final MutableFloat forgetMax;
        protected transient float forgetMaxCyclesCached = Float.NaN;
        private float forgetCyclesMaxMinRange;

        /**
         *
         * @param forgetTimeMin minimum forgetting time
         * @param forgetTimeMax maximum forgetting time
         * @param perfection
         */
        public LinearForget(@NotNull MutableFloat forgetTimeMin, @NotNull MutableFloat forgetTimeMax, @NotNull MutableFloat perfection) {
            super(forgetTimeMin, perfection);
            this.forgetMax = forgetTimeMax;
        }

        @Override
        public void update(@NotNull NAR nar) {
            super.update(nar);
            this.forgetMaxCyclesCached = forgetMax.floatValue();
            this.forgetCyclesMaxMinRange = forgetMaxCyclesCached - forgetCyclesCached;
        }

        @Override
        public void accept(@NotNull BLink<? extends X> budget) {

            final float currentPriority = budget.pri();
            final float forgetDeltaCycles = budget.setLastForgetTime(now);
            if (forgetDeltaCycles == 0) {
                return;
            }

            float minPriorityForgettingCanAffect = this.perfectionCached * budget.qua(); //relativeThreshold

            if (currentPriority < minPriorityForgettingCanAffect) {
                //priority already below threshold, don't decrease any further
                return ;
            }

            //more durability = slower forgetting; durability near 1.0 means forgetting will happen at slowest decided by the forget rate,
            // lower values approaching 0.0 means will happen at faster rates
            float forgetProportion = forgetCyclesCached + forgetCyclesMaxMinRange * (1.0f - budget.dur());


            float newPriority;
            if (forgetProportion >= 1.0f) {
                //total drain; simplification of the complete LERP formula
                newPriority = minPriorityForgettingCanAffect;
            } else if (forgetProportion <= 0f) {
                //??
                newPriority = currentPriority;
            } else {
                //LERP between current value and minimum
                newPriority = currentPriority * (1.0f - forgetProportion) +
                        minPriorityForgettingCanAffect * (forgetProportion);
            }

            //if (Math.abs(newPriority - currentPriority) > Global.BUDGET_EPSILON)
            budget.setPriority(newPriority);

        }


    }


    /** exponential decay in proportion to time since last forget.
     *  provided by TonyLo as used in the ALANN system. */
    public final static class ExpForget<X> extends AbstractForget<X> {

        public ExpForget(@NotNull MutableFloat forgetTime, @NotNull MutableFloat perfection) {
            super(forgetTime, perfection);
        }

        @Override
        public void accept(@NotNull BLink<? extends X> budget) {

            float dt = budget.setLastForgetTime(now);

            float threshold = budget.qua() * perfectionCached;
            //if (dt > 0) {

                float p = budget.priIfFiniteElseZero();


                if (p > threshold) {

                    //Exponential decay
                    p *= (float) Math.exp(
                            -((1.0f - budget.dur()) / forgetCyclesCached) * dt
                    );

                }

                budget.setPriority(Math.max(threshold, p));
            //}
        }

    }

    //TODO implement as a Forgetter:
    public static final Predicate<BLink<?>> simpleForgetDecay = (b) -> {
        float p = b.pri() * 0.95f;
        if (p > b.qua()*0.1f)
            b.setPriority(p);
        return true;
    };
}
