package nars.test.agent;

import jcog.data.FloatParam;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.ActionConcept;
import nars.concept.SensorConcept;

import static nars.Op.BELIEF;


/**
 * 1 input x 1 output environment
 */
public class Line1DSimplest extends NAgent {

    public static final float resolution = 0.01f;
    public final SensorConcept in;

    /** the target value */
    public final FloatParam i = new FloatParam(0.5f, 0, 1f);

    public final ActionConcept out;
    /** the current value */
    public final FloatParam o = new FloatParam(0.5f, 0, 1f);


    public Line1DSimplest(NAR n) {
        super("L", n);

        in = senseNumber( $.func("i", id), this.i);

        out = action( $.func("o", id), (b, d) -> {

            if (d != null) {
                float f = d.freq();
//                float inc = ((f - 0.5f) * 2f) * 0.04f;
//                o.setValue(unitize(o.getValue() + inc));

                o.setValue(f);

//                else {
//                    //no change
//                    return null;
//                }


            }

            return $.t(o.floatValue(), nar.confidenceDefault(BELIEF));
        });

        in.resolution(resolution);

    }

    @Override
    protected float act() {
        if (o != o)
            return Float.NaN;

        float dist = Math.abs(i.floatValue() - o.floatValue());

        //return (1f - dist); //unipolar, 0..1.0
        return ((1f - dist) * 2f) - 1f; //bipolar, normalized to -1..+1
    }




}
