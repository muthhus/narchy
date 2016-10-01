package nars;

import nars.concept.SensorConcept;
import nars.term.Compound;
import nars.truth.Truth;
import nars.util.Util;
import nars.util.math.FloatSupplier;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;

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


    default SensorConcept sense(String term, BooleanSupplier value) {
        return sense(term, () -> value.getAsBoolean() ? 1f : 0f);
    }

    default SensorConcept sense(String term, FloatSupplier value) {
        return sense(term, value, nar().truthResolution.floatValue(), (v) -> $.t(v, alpha()) );
    }

    default SensorConcept sense(String term, FloatSupplier value, float resolution, FloatToObjectFunction<Truth> truthFunc) {
        return sense($.$(term), value, resolution, truthFunc);
    }

    default SensorConcept sense(Compound term, FloatSupplier value, float resolution, FloatToObjectFunction<Truth> truthFunc) {
        SensorConcept s = new SensorConcept(term, nar(), value, truthFunc);
        s.resolution(resolution);

        sensors().add( s );
        return s;
    }

    /** learning rate */
    default float alpha() {
        return nar().confidenceDefault(Symbols.BELIEF);
    }

    /** interpret an int as a selector between enumerated values */
    default <E extends Enum> void senseSwitch(String term, Supplier<E> value) {
        E[] values = ((Class<? extends E>)value.get().getClass()).getEnumConstants();
        for (E e : values) {
            String t = switchTerm(term, e.toString());
            sense(t, ()->value.get()==e);
        }
    }

    static String switchTerm(String term, String e) {
        //return "(" + e + " --> " + term + ")";
        return  "(" + term + " , " + e + ")";
    }

    default void senseSwitch(String term, IntSupplier value, int min, int max) {
        senseSwitch(term, value, Util.intSequence(min,max));
    }

    /** interpret an int as a selector between (enumerated) integer values */
    default void senseSwitch(String term, IntSupplier value, int[] values) {
        for (int e : values) {
            String t = switchTerm(term, String.valueOf(e));
            sense(t, ()->value.getAsInt()==e);
        }
    }

    /** interpret an int as a selector between (enumerated) object values */
    default <O> void senseSwitch(String term, Supplier<O> value, O... values) {
        for (O e : values) {
            String t = switchTerm(term, "\"" + e.toString() + "\"");
            sense(t, ()->value.get().equals(e));
        }
    }


}
