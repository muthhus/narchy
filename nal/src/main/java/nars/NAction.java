package nars;

import nars.concept.ActionConcept;
import nars.time.Tense;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;

import java.util.Collection;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Created by me on 9/30/16.
 */
public interface NAction {

    Collection<ActionConcept> actions();

    NAR nar();

    /** latches to either one of 2 states until it shifts to the other one. suitable for representing
     * push-buttons like keyboard keys. by default with no desire the state is off.  the 'on' and 'off'
     * procedures will be called only as necessary (when state changes).  the off procedure will not be called immediately.
     * its initial state will remain indetermined until the first feedback is generated.
     * */
    default ActionConcept actionToggle(String s, Runnable on, Runnable off) {

        final int[] state = { 0 }; // 0: unknown, -1: false, +1: true

        ActionConcept m = new ActionConcept(s, nar(), (b, d) -> {
            int now = state[0];
            boolean next = d!=null && d.freq() >= 0.5f;
            float alpha = nar().confidenceDefault(Symbols.BELIEF);
            if (now>=0 && !next) {
                state[0] = -1; off.run(); return $.t(0, alpha);
            } else if (now<=0 && next) {
                state[0] = +1; on.run(); return $.t(1f, alpha);
            }
            return null;
        });

        actions().add(m);
        return m;
    }
    /** latches to either one of 2 states until it shifts to the other one. suitable for representing
     * push-buttons like keyboard keys. by default with no desire the state is off.  the 'on' and 'off'
     * procedures will be called only as necessary (when state changes).  the off procedure will not be called immediately.
     * its initial state will remain indetermined until the first feedback is generated.
     * */
    default ActionConcept actionTriState(String s, IntConsumer i) {


        ActionConcept m = new ActionConcept(s, nar(), (b, d) -> {
            float deadZoneFreq = 1f/4;

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
            i.accept(ii);

            float f;
            switch (ii) {
                case 1:
                    f = 1f; break;
                case 0:
                    f = 0.5f; break;
                case -1:
                    f = 0f; break;
                default:
                    throw new RuntimeException();
            }
            return $.t(f, nar().confidenceDefault(Symbols.BELIEF));
        });

        actions().add(m);
        return m;
    }

    default ActionConcept actionToggle(String s, BooleanProcedure onChange) {
        return actionToggle(s, () -> onChange.value(true), () -> onChange.value(false) );
    }
    default ActionConcept actionToggleRapid(String s, BooleanProcedure onChange, int minPeriod) {
        return actionToggleRapid(s, () -> onChange.value(true), () -> onChange.value(false), minPeriod );
    }

    /**
     * rapid-fire pushbutton with a minPeriod after which it is reset to off, allowing
     * re-triggering to ON while the true state remains enabled
     *
     * TODO generalize to actionPWM (pulse width modulation) with controllable reset period (ex: by frequency, or conf etc)
     * */
    default ActionConcept actionToggleRapid(String term, Runnable on, Runnable off, int minPeriod) {

        if (minPeriod < 1)
            throw new UnsupportedOperationException();

        final long[] reset = { Tense.ETERNAL }; //last enable time
        final int[] state = { 0 }; // 0: unknown, -1: false, +1: true

        ActionConcept m = new ActionConcept(term, nar(), (b, d) -> {

            boolean next = d!=null && d.freq() >= 0.5f;

            float alpha = nar().confidenceDefault(Symbols.BELIEF);
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

            if (state[0]!=s) {
                if (s < 0)
                    off.run();
                else
                    on.run();
                state[0] = s;
            }

            return $.t(v, alpha);
        }) {
            protected boolean alwaysUpdateFeedback() { return true; }
        };

        actions().add(m);
        return m;
    }

    /** the supplied value will be in the range -1..+1. if the predicate returns false, then
     * it will not allow feedback through. this can be used for situations where the action
     * hits a limit or boundary that it did not pass through.
     *
     * TODO make a FloatToFloatFunction variation in which a returned value in 0..+1.0 proportionally decreasese the confidence of any feedback
     */
    default ActionConcept actionBipolar(String s, FloatPredicate update) {

        ActionConcept m = new ActionConcept(s, nar(), (b, d) -> {
            if (d!=null) {
                float f = d.freq();
                float y = (f - 0.5f) * 2f;
                float alpha = nar().confidenceDefault(Symbols.BELIEF);
                if (update.accept(y)) {
                    return $.t(f, alpha);
                } else {
                    return $.t(0.5f, alpha); //neutral on failure
                }
            }
            return null;
        });

        actions().add(m);
        return m;
    }

    default ActionConcept actionRangeIncrement(String s, IntSupplier in, int dx, int min, int max, IntConsumer out) {
        //TODO
        return null;
    }

}
