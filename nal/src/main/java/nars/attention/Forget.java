package nars.attention;

import jcog.Util;
import nars.Param;
import nars.budget.BLink;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Created by me on 9/4/16.
 */
public class Forget<X> implements Consumer<BLink<X>> {

    public final float r;

    public final float maxEffectiveQuality;

    public final float gain;

    public Forget(float r) {
        this(r, 1f ,1f);
    }

    public Forget(float r, float maxEffectiveQuality, float gain) {
        this.r = r;
        this.maxEffectiveQuality = maxEffectiveQuality;
        this.gain = gain;
    }

    @Nullable
    public static <X> Consumer<BLink<X>> forget(int s, float p, float m, FloatToObjectFunction<Consumer<BLink<X>>> f) {

        float r = p > 0 ?
                -((s * Param.BAG_THRESHOLD) - p - m) / m :
                0;

        //float pressurePlusOversize = pressure + Math.max(0, expectedAvgMass * size - existingMass);
        //float r = (pressurePlusOversize) / (pressurePlusOversize + existingMass*4f /* momentum*/);

        //System.out.println(pressure + " " + existingMass + "\t" + r);
        return r >= (Param.BUDGET_EPSILON/s) ? f.valueOf(Util.unitize(r)) : null;
    }

    @Override
    public void accept(@NotNull BLink<X> bLink) {
        float p = bLink.priSafe(-1);
        if (p > 0) {
            float q = bLink.qua();
            //if (q==q) //???
                bLink.setPriority(gain * p * (1f - (r * (1f - q * maxEffectiveQuality))));
            //else
                //bLink.delete();
            //bLink.setPriority(p - (r * (1f - q * maxEffectiveQuality)));

        }
    }

}

//package nars.budget.forget;
//
//import nars.Global;
//import nars.NAR;
//import nars.link.BLink;
//import nars.nal.Tense;
//import nars.util.Util;
//import org.apache.commons.lang3.mutable.MutableFloat;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.function.Predicate;
//
//
///**
// * Utility methods for Attention Forgetting processes
// */
//public enum Forget {
//    ;
//
//    /**
//     * acts as a filter to decide if an element should remain in a bag, otherwise some forgetting modification an be applied to a retained item
//     */
//    public interface BudgetForgetFilter extends Predicate<BLink>, BudgetForget {
//        /**
//         * called each frame to update parameters
//         */
//        @Override
//        void update(@NotNull NAR nar);
//
//
//    }
//
////    /** for BLinked budgeted items: if that item becomes Deleted, then the enclosing BLink is removed during a Bag.filter operation that applies this Predicate */
////    public static final class ForgetAndDetectDeletion implements BudgetForgetFilter {
////
////        final BudgetForget forget;
////
////        public ForgetAndDetectDeletion(BudgetForget forget) {
////            this.forget = forget;
////        }
////
////        @Override
////        public boolean test(@NotNull BLink b) {
////            //assert(!b.isDeleted());
////            if (((Budgeted)b.get()).isDeleted()) {
////                b.delete();
////                return false;
////            }
////            forget.accept(b);
////            return true;
////        }
////
////        @Override
////        public void accept(BLink bLink) {
////            forget.accept(bLink);
////        }
////
////        @Override
////        public final void update(@NotNull NAR nar) {
////            forget.update(nar);
////        }
////
////        @Override
////        public final void cycle(float subFrame) {
////            forget.cycle(subFrame);
////        }
////
////    }
//
//    public abstract static class AbstractForget implements BudgetForget {
//
//        @NotNull
//        public final MutableFloat forgetCycles;
//        @NotNull
//        public final MutableFloat perfection;
//
//        //cached values for fast repeated accesses
//
//        /**
//         * cached value of # cycles equivalent of the supplied forget durations parameter
//         */
//        protected transient float forgetCyclesCached = Float.NaN;
//        protected transient float perfectionCached = Float.NaN;
//        protected transient float now = Float.NaN;
//        protected transient float subFrame = Float.NaN;
//        private long frame = Tense.TIMELESS;
//
//        public AbstractForget(@NotNull MutableFloat forgetCycles, @NotNull MutableFloat perfection) {
//            this.forgetCycles = forgetCycles;
//            this.perfection = perfection;
//        }
//
//        @Override
//        public abstract void accept(@NotNull BLink budget);
//
//        @Override
//        public void update(@Nullable NAR nar) {
//            //same for duration of the cycle
//            forgetCyclesCached = forgetCycles.floatValue();
//            perfectionCached = perfection.floatValue();
//            if (nar!=null)
//                this.now = frame = nar.time();
//        }
//
//        @Override
//        public void cycle(float subFrame) {
//            this.now = (this.subFrame = subFrame) + frame;
//        }
//
//
//        public AbstractForget setForgetCycles(float f) {
//            forgetCycles.setValue(f); //not necessary unless we want access to this value as a MutableFloat from elsewhere
//            forgetCyclesCached = f;
//            return this;
//        }
//    }
//
//
////    /**
////     * linaer decay in proportion to time since last forget
////     */
////    public static class LinearForget extends AbstractForget {
////
////        @NotNull
////        private final MutableFloat forgetMax;
////        protected transient float forgetMaxCyclesCached = Float.NaN;
////        private float forgetCyclesMaxMinRange;
////
////        /**
////         * @param forgetTimeMin minimum forgetting time
////         * @param forgetTimeMax maximum forgetting time
////         * @param perfection
////         */
////        public LinearForget(@NotNull MutableFloat forgetTimeMin, @NotNull MutableFloat forgetTimeMax, @NotNull MutableFloat perfection) {
////            super(forgetTimeMin, perfection);
////            this.forgetMax = forgetTimeMax;
////        }
////
////        @Override
////        public void update(@NotNull NAR nar) {
////            super.update(nar);
////            this.forgetMaxCyclesCached = forgetMax.floatValue();
////            this.forgetCyclesMaxMinRange = forgetMaxCyclesCached - forgetCyclesCached;
////        }
////
////        @Override
////        public void accept(@NotNull BLink budget) {
////
////            final float currentPriority = budget.pri();
////            final float forgetDeltaCycles = budget.setLastForgetTime(now);
////            if (forgetDeltaCycles == 0) {
////                return;
////            }
////
////            float minPriorityForgettingCanAffect = this.perfectionCached * budget.qua(); //relativeThreshold
////
////            if (currentPriority < minPriorityForgettingCanAffect) {
////                //priority already below threshold, don't decrease any further
////                return;
////            }
////
////            //more durability = slower forgetting; durability near 1.0 means forgetting will happen at slowest decided by the forget rate,
////            // lower values approaching 0.0 means will happen at faster rates
////            float forgetProportion = forgetCyclesCached + forgetCyclesMaxMinRange * (1.0f - budget.dur());
////
////
////            float newPriority;
////            if (forgetProportion >= 1.0f) {
////                //total drain; simplification of the complete LERP formula
////                newPriority = minPriorityForgettingCanAffect;
////            } else if (forgetProportion <= 0f) {
////                //??
////                newPriority = currentPriority;
////            } else {
////                //LERP between current value and minimum
////                newPriority = currentPriority * (1.0f - forgetProportion) +
////                        minPriorityForgettingCanAffect * (forgetProportion);
////            }
////
////            //if (Math.abs(newPriority - currentPriority) > Global.BUDGET_EPSILON)
////            budget.setPriority(newPriority);
////
////        }
////
////
////    }
//
//
//    /**
//     * exponential decay in proportion to time since last forget.
//     * provided by TonyLo as used in the ALANN system.
//     */
//    public final static class ExpForget extends AbstractForget {
//
////        public ExpForget(@NotNull MutableFloat perfection) {
////            this(new MutableFloat(0), perfection);
////        }
//        public ExpForget(@NotNull MutableFloat forgetTime, @NotNull MutableFloat perfection) {
//            super(forgetTime, perfection);
//        }
//
//        @Override
//        public void accept(@NotNull BLink budget) {
//
//            float p0 = budget.pri();
//            if (p0 != p0) /* NaN, deleted */
//                return;
//
//            float last = budget.getLastForgetTime();
//            if (last!=last) {
//                last = now; //NaN, first time
//            }
//
//            float dt = now - last;
//
//            float threshold = budget.qua() * perfectionCached;
//
//            float p = p0;
//
//            if (dt > 0 && p > threshold) {
//
//                //Exponential decay
//                p *= (float) Math.exp(
//                        -((1.0f - budget.dur()) / forgetCyclesCached) * dt
//                );
//
//            }
//
//            if (p < threshold)
//                p = threshold;
//
//            if (dt==0 || !Util.equals(p, p0, Global.BUDGET_EPSILON))
//                budget.setPriority(p, now);
//
//            //}
//        }
//
//    }
//
//    /**
//     * exponential decay in proportion to time since last forget.
//     * provided by TonyLo as used in the ALANN system.
//     */
//    public final static class LinearForget extends AbstractForget {
//
//        //        public ExpForget(@NotNull MutableFloat perfection) {
////            this(new MutableFloat(0), perfection);
////        }
//        public LinearForget(@NotNull MutableFloat forgetTime, @NotNull MutableFloat perfection) {
//            super(forgetTime, perfection);
//        }
//
//        @Override
//        public void accept(@NotNull BLink budget) {
//
//            float p0 = budget.pri();
//            if (p0 != p0) /* NaN, deleted */
//                return;
//
//            float last = budget.getLastForgetTime();
//            if (last!=last) {
//                last = now; //NaN, first time
//            }
//
//            float dt = now - last;
//
//            float threshold = budget.qua() * perfectionCached;
//
//            float p = p0;
//
//            if (dt > 0 && p > threshold) {
//
//
//                float forgetProportion;
//                if (p < threshold) {
//                    forgetProportion = 0;
//                } else {
//
//                    //more durability = slower forgetting; durability near 1.0 means forgetting will happen at slowest decided by the forget rate,
//                    // lower values approaching 0.0 means will happen at faster rates
//                    forgetProportion = dt / forgetCyclesCached * (1.0f - budget.dur());
//                }
//
//
//                if (forgetProportion >= 1.0f) {
//                    //total drain; simplification of the complete LERP formula
//                    p = threshold;
//                } else if (forgetProportion <= 0f) {
//                    //??
//                    //p = p;
//                } else {
//                    //LERP between current value and minimum
//                    p = p * (1.0f - forgetProportion);
//
//
//                }
//            }
//
//            if (p < threshold)
//                p = threshold;
//
//            if (dt==0 || !Util.equals(p, p0, Global.BUDGET_EPSILON))
//                budget.setPriority(p, now);
//
//            //}
//        }
//
//    }
//
//
//    /**
//     * sets the priority value to the quality value
//     */
//    public final static BudgetForget QualityToPriority = new BudgetForget() {
//
//        @Override
//        public void accept(@NotNull BLink budget) {
//            budget.setPriority(budget.qua());
//        }
//
//        @Override
//        public void update(@NotNull NAR nar) {
//
//        }
//
//        @Override
//        public void cycle(float subFrame) {
//
//        }
//    };
//
//
//    //TODO implement as a Forgetter:
//    public static final Predicate<BLink<?>> simpleForgetDecay = (b) -> {
//        float p = b.pri() * 0.95f;
//        if (p > b.qua() * 0.1f)
//            b.setPriority(p);
//        return true;
//    };
//
//}
