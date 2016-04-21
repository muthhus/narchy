package nars.util.signal;

import nars.NAR;
import nars.Narsese;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

/**
 * SensorConcept which wraps a MutableFloat value
 */
public class FloatConcept extends SensorConcept {

    @NotNull
    private final MutableFloat value;

    public FloatConcept(@NotNull String compoundTermString, @NotNull NAR n) throws Narsese.NarseseException {
        this(compoundTermString, n, Float.NaN);
    }

    public FloatConcept(@NotNull String compoundTermString, @NotNull NAR n, float initialValue) throws Narsese.NarseseException {
        this(compoundTermString, n, new MutableFloat(initialValue));
    }

    public FloatConcept(@NotNull String compoundTermString, @NotNull NAR n, @NotNull MutableFloat v) throws Narsese.NarseseException {
        super(compoundTermString, n, v);
        this.value = v;
    }

    public float set(float v) {
        value.setValue(v);
        return v;
    }

    @NotNull @Override public FloatConcept punc(char c) {
        super.punc(c);
        return this;
    }
}
