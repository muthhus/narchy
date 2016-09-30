package nars;

import nars.concept.ActionConcept;
import nars.concept.SensorConcept;
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

    default ActionConcept actionToggle(String s, BooleanProcedure onChange) {
        return actionToggle(s, () -> onChange.value(true), () -> onChange.value(false) );
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
