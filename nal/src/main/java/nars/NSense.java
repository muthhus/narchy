package nars;

import nars.concept.SensorConcept;
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
                (v) -> $.t(v, nar().confidenceDefault(Symbols.BELIEF)));
        s.resolution(resolution);

        sensors().add( s );
    }

    default <E extends Enum> void senseEnum(String term, Supplier<E> value) {
        E[] values = ((Class<? extends E>)value.get().getClass()).getEnumConstants();
        for (E e : values) {
            String t = "(" + term + "-->[" + e.toString() + "])";
            sense(t, ()->value.get()==e);
        }
    }

    /** interpret an int as a selector of enumerated integer value */
    default void senseEnum(String term, IntSupplier value, int[] values) {
        for (int e : values) {
            String t = "(" + term + "-->[" + String.valueOf(e) + "])";
            sense(t, ()->value.getAsInt()==e);
        }
    }
}
