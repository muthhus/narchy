package nars;

import jcog.Util;
import jcog.data.FloatParam;
import nars.concept.ActionConcept;
import nars.concept.GoalActionConcept;
import nars.term.Compound;
import nars.time.Tense;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;

import static nars.Op.BELIEF;

/**
 * Created by me on 9/30/16.
 */
public interface NAct {

    @NotNull Collection<ActionConcept> actions();

    NAR nar();

    /** master curiosity factor, for all actions */
    FloatParam curiosity();

    /**
     * latches to either one of 2 states until it shifts to the other one. suitable for representing
     * push-buttons like keyboard keys. by default with no desire the state is off.  the 'on' and 'off'
     * procedures will be called only as necessary (when state changes).  the off procedure will not be called immediately.
     * its initial state will remain indetermined until the first feedback is generated.
     */
    @Nullable
    default ActionConcept actionToggle(@NotNull Compound s, @NotNull Runnable on, @NotNull Runnable off) {
        ActionConcept m = new GoalActionConcept(s, this, (b, d) -> {
            boolean next = d != null && d.freq() > 0.5f;
            float conf = d != null ? d.conf() : nar().confMin.floatValue();
            return toggle(on, off, next, conf);
        });
        actions().add(m);

        m.resolution.setValue(1f);

        return m;
    }

//    /** softmax-like signal corruption that emulates PWM (pulse-width modulation) modulated by desire frequency */
//    @Nullable default ActionConcept actionTogglePWM(@NotNull Compound s, @NotNull Runnable on, @NotNull Runnable off) {
//        ActionConcept m = new GoalActionConcept(s, this, (b, d) -> {
//            float df = d != null ? d.freq() : 0.5f;
//            boolean corrupt = nar().random().nextFloat() > Math.abs(df - 0.5f) * 2f;
//
//            boolean next = df > 0.5f;
//            if (corrupt) next = !next;
//
//            return toggle(on, off, next);
//        });
//
//        actions().add(m);
//        return m;
//    }

    @Nullable
    default Truth toggle(@NotNull Runnable on, @NotNull Runnable off, boolean next, float conf) {
        float freq;
        if (next) {
            freq = +1;
            on.run();
        } else {
            freq = 0f;
            off.run();
        }

        return $.t(freq,
                conf);
                //nar().confDefault(BELIEF) /*d.conf()*/);
    }

    /**
     * selects one of 2 states until it shifts to the other one. suitable for representing
     * push-buttons like keyboard keys. by default with no desire the state is off.   the off procedure will not be called immediately.
     */
    @Nullable
    default ActionConcept actionTriState(@NotNull Compound s, @NotNull IntConsumer i) {
        return actionTriState(s, (v) -> { i.accept(v); return true; } );
    }

    @Nullable
    default ActionConcept actionTriState(@NotNull Compound s, @NotNull IntPredicate i) {

        GoalActionConcept m = new GoalActionConcept(s, this, (b, d) -> {
            float deadZoneFreq =
                     1f/6;
                    // 1f/4;
            //1f/3f;


            int ii;
            if (d == null) {
                ii = 0;
            } else {
                float f = d.freq();
                if (f > 0.5f + deadZoneFreq)
                    ii = +1;
                else if (f < 0.5f - deadZoneFreq)
                    ii = -1;
                else
                    ii = 0;
            }

            boolean accepted = i.test(ii);

            float f;
            switch (ii) {
                case 1:
                    f = 1f;
                    break;
                case 0:
                    f = 0.5f;
                    break;
                case -1:
                    f = 0f;
                    break;
                default:
                    throw new RuntimeException();
            }

            return accepted ?
                    $.t(f,
                            d!=null ? d.conf() :
                                nar().confMin.floatValue()
                                //nar().confDefault(BELIEF)
                    )
                    : null
                    ;
        });
        m.resolution.setValue(0.5f);

        actions().add(m);

        return m;
    }

    @Nullable
    default ActionConcept actionTriStatePWM(@NotNull Compound s, @NotNull IntConsumer i) {


        ActionConcept m = new GoalActionConcept(s, this, (b, d) -> {


            int ii;
            if (d == null) {
                ii = 0;
            } else {
                float f = d.freq();
                if (f == 1f) {
                    ii = +1;
                } else if (f == 0) {
                    ii = -1;
                } else if (f > 0.5f) {
                    ii = nar().random().nextFloat() <= ((f - 0.5f)*2f) ? +1 : 0;
                } else if (f < 0.5f) {
                    ii = nar().random().nextFloat() <= ((0.5f - f)*2f) ? -1 : 0;
                } else
                    ii = 0;
            }

            i.accept(ii);

            float f;
            switch (ii) {
                case 1:
                    f = 1f;
                    break;
                case 0:
                    f = 0.5f;
                    break;
                case -1:
                    f = 0f;
                    break;
                default:
                    throw new RuntimeException();
            }

            return
                    //d!=null ?
                    $.t(f,
                            //d.conf()
                            nar().confDefault(BELIEF)
                    )
                    //: null
                    ;
        });

        actions().add(m);
        return m;
    }

    @Nullable
    default ActionConcept actionToggle(@NotNull Compound s, @NotNull BooleanProcedure onChange) {
        return actionToggle(s, () -> onChange.value(true), () -> onChange.value(false));
    }

    @Nullable
    default ActionConcept actionToggleRapid(@NotNull Compound s, @NotNull BooleanProcedure onChange, int minPeriod) {
        return actionToggleRapid(s, () -> onChange.value(true), () -> onChange.value(false), minPeriod);
    }

    /**
     * rapid-fire pushbutton with a minPeriod after which it is reset to off, allowing
     * re-triggering to ON while the true state remains enabled
     * <p>
     * TODO generalize to actionPWM (pulse width modulation) with controllable reset period (ex: by frequency, or conf etc)
     */
    @Nullable
    default ActionConcept actionToggleRapid(@NotNull Compound term, @NotNull Runnable on, @NotNull Runnable off, int minPeriod) {

        if (minPeriod < 1)
            throw new UnsupportedOperationException();

        final long[] reset = {Tense.ETERNAL}; //last enable time
        final int[] state = {0}; // 0: unknown, -1: false, +1: true

        ActionConcept m = new GoalActionConcept(term, this, (b, d) -> {

            boolean next = d != null && d.freq() >= 0.5f;

            float alpha = nar().confDefault(BELIEF);
            int v;
            int s;
            if (!next) {
                reset[0] = Tense.ETERNAL;
                s = -1;
                v = 0;
            } else {

                long lastReset = reset[0];
                long now = nar().time();
                if (lastReset == Tense.ETERNAL) {
                    reset[0] = now;
                    s = -1;
                } else {
                    if ((now - lastReset) % minPeriod == 0) {
                        s = -1;
                    } else {
                        s = +1;
                    }
                }
                v = 1;
            }

            if (state[0] != s) {
                if (s < 0)
                    off.run();
                else
                    on.run();
                state[0] = s;
            }

            return $.t(v, alpha);
        });

        actions().add(m);
        return m;
    }

    /**
     * the supplied value will be in the range -1..+1. if the predicate returns false, then
     * it will not allow feedback through. this can be used for situations where the action
     * hits a limit or boundary that it did not pass through.
     * <p>
     * TODO make a FloatToFloatFunction variation in which a returned value in 0..+1.0 proportionally decreasese the confidence of any feedback
     */
    @NotNull
    default ActionConcept action(@NotNull String s, @NotNull GoalActionConcept.MotorFunction update) throws Narsese.NarseseException {
        return action($.$(s), update);
    }

    @NotNull
    default ActionConcept action(@NotNull Compound s, @NotNull GoalActionConcept.MotorFunction update) {
        ActionConcept m = new GoalActionConcept(s, this, update);
        actions().add(m);
        return m;
    }

    /**
     * the supplied value will be in the range -1..+1. if the predicate returns false, then
     * it will not allow feedback through. this can be used for situations where the action
     * hits a limit or boundary that it did not pass through.
     * <p>
     * TODO make a FloatToFloatFunction variation in which a returned value in 0..+1.0 proportionally decreasese the confidence of any feedback
     */
    @NotNull
    default ActionConcept actionBipolar(@NotNull Compound s, @NotNull FloatPredicate update) {
        return actionUnipolar(s, (f) -> {
            float y = (f - 0.5f) * 2f;
            return update.accept(y);
        });
    }

    /**
     * update function receives a value in 0..1.0 corresponding directly to the present goal frequency
     */
    @NotNull
    default ActionConcept actionUnipolar(@NotNull Compound s, @NotNull FloatPredicate update) {
        return action(s, (b, d) -> {
            if (d != null) {
                float conf =
                        //nar().confDefault(BELIEF);
                        d.conf();
                float f = d.freq();
                if (update.accept(f)) {
                    return $.t(f, conf);
                } else {
                    return $.t(0.5f, conf); //neutral on failure
                    //return null;
                }
            }
            return null;
        });
    }

    @NotNull
    default ActionConcept actionUnipolarTransfer(@NotNull Compound s, @NotNull FloatToFloatFunction update) {
        return action(s, (b, d) -> {
            if (d != null) {
                float conf = d.conf();
                float f = d.freq();
                f = Util.unitize(update.valueOf(f));
                if (f == f) {
                    return $.t(f, conf);
                }
            }
            return b;
        });
    }

    default ActionConcept actionLerp(Compound s, FloatProcedure g, float min, float max) {
        return actionUnipolar(s, (f) -> {
            g.value(Util.lerp(f, max, min));
            return true;
        });
    }

    @Nullable
    default ActionConcept actionRangeIncrement(String s, IntSupplier in, int dx, int min, int max, IntConsumer out) {
        //TODO
        return null;
    }

}
