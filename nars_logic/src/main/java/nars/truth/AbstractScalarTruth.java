package nars.truth;

import nars.Global;
import nars.util.data.Util;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 7/4/15.
 */
public abstract class AbstractScalarTruth extends AbstractTruth<Float> implements Truth {


    /**
     * The frequency factor of the truth value
     */
    private float frequency;


    @Override
    public void setConfidence(float b) {
        float e = Global.TRUTH_EPSILON; //getEpsilon();
        confidence = Util.round(b, e);
    }


    @Override
    public final int hashCode() {
        return Truth.hash(this);
    }


    @Override
    public float getFrequency() {
        return frequency;
    }


    @NotNull
    @Override
    public Truth setFrequency(float f) {
        float e = Global.TRUTH_EPSILON; //getEpsilon();
        frequency = Util.round(f, e);
        return this;
    }



    @Override
    public boolean equalsFrequency(@NotNull Truth t) {
        return (Util.equal(frequency, t.getFrequency(), Global.TRUTH_EPSILON));
    }
}
