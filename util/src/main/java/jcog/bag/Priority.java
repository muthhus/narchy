package jcog.bag;

import org.jetbrains.annotations.NotNull;

import static jcog.Util.lerp;
import static jcog.Util.unitize;

/**
 * Created by me on 2/17/17.
 */
public interface Priority extends Prioritized {

    default void priAdd(float toAdd) {
        setPriority(priSafe(0) + toAdd);
    }

    default void priSub(float toSubtract) { setPriority(priSafe(0) - toSubtract); }

    static float validPriority(float p) {
        if (p!=p /* fast NaN test */)
            throw new PriorityException();
        if (p > 1.0f)
            p = 1.0f;
        else if (p < 0.0f)
            p = 0.0f;
        return p;
    }

    /**
     * Change priority value
     *
     * @param p The new priority
     * @return whether the operation had any effect
     */
    void setPriority(float p);

    //    default Budget mult(float priFactor, float durFactor, float quaFactor) {
    //        if (priFactor!=1) priMult(priFactor);
    //        if (durFactor!=1) durMult(durFactor);
    //        if (quaFactor!=1) quaMult(quaFactor);
    //        return this;
    //    }
    //

    @NotNull
    default Priority priMult(float factor) {
        float p = pri();
        if (p==p)
            setPriority(p * factor);
        return this;
    }


    @NotNull
    default Priority priLerp(float target, float speed) {
        setPriority(lerp(speed, target, pri()));
        return this;
    }

    /** returns the delta */
    default float priLerpMult(float factor, float speed) {

//        if (Util.equals(factor, 1f, Param.BUDGET_EPSILON))
//            return 0; //no change

        float p = pri();
        float target = unitize(p * factor);
        float delta = target - p;
        setPriority(lerp(speed, target, p));
        return delta;

    }

//    void orPriority(float v);
//
//    void orPriority(float x, float y);

    final class PriorityException extends RuntimeException {
        public PriorityException() {
            super("NaN");
        }
        public PriorityException(String message) {
            super(message);
        }

        @Override
        public Throwable fillInStackTrace() {
            return null;
        }
    }

}
