package nars;

import nars.concept.SensorConcept;
import nars.util.Util;
import nars.util.math.FloatSupplier;

import java.util.Collection;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Created by me on 9/30/16.
 */
public interface NSense {

    Collection<SensorConcept> sensors();

    NAR nar();


    default void sense(String term, BooleanSupplier value) {
        sense(term, () -> value.getAsBoolean() ? 1f : 0f, nar().truthResolution.floatValue());
    }

    default void sense(String term, FloatSupplier value) {
        sense(term, value, nar().truthResolution.floatValue());
    }

    default void sense(String term, FloatSupplier value, float resolution) {
        SensorConcept s = new SensorConcept(term, nar(), value,
                (v) -> $.t(v, alpha()));
        s.resolution(resolution);

        sensors().add( s );
    }

    /** learning rate */
    default float alpha() {
        return nar().confidenceDefault(Symbols.BELIEF);
    }

    /** interpret an int as a selector between enumerated values */
    default <E extends Enum> void senseSwitch(String term, Supplier<E> value) {
        E[] values = ((Class<? extends E>)value.get().getClass()).getEnumConstants();
        for (E e : values) {
            String t = "(" + term + "-->[" + e.toString() + "])";
            sense(t, ()->value.get()==e);
        }
    }

    default void senseSwitch(String term, IntSupplier value, int min, int max) {
        senseSwitch(term, value, Util.intSequence(min,max));
    }

    /** interpret an int as a selector between (enumerated) integer values */
    default void senseSwitch(String term, IntSupplier value, int[] values) {
        for (int e : values) {
            String t = "(" + term + "-->[" + String.valueOf(e) + "])";
            sense(t, ()->value.getAsInt()==e);
        }
    }

    /** interpret an int as a selector between (enumerated) object values */
    default <O> void senseSwitch(String term, Supplier<O> value, O... values) {
        for (O e : values) {
            String t = "(" + term + "-->[\"" + String.valueOf(e) + "\"])";
            sense(t, ()->value.get().equals(e));
        }
    }

}
