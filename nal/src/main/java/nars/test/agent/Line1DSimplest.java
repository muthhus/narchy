package nars.test.agent;

import jcog.Util;
import jcog.data.FloatParam;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.ActionConcept;
import nars.concept.SensorConcept;


/**
 * 1 input x 1 output environment
 */
public class Line1DSimplest extends NAgent {

    public static final float resolution = 0.04f;
    //public final SensorConcept in;

    /**
     * the target value
     */
    public final FloatParam i = new FloatParam(0.5f, 0, 1f);

    public final ActionConcept out;
    /**
     * the current value
     */
    public final FloatParam o = new FloatParam(0.5f, 0, 1f);
    public final SensorConcept in;


    public Line1DSimplest(NAR n) {
        super("L", n);

        in = senseNumber(
                //$.inh($.the("i"), id),
                $.inh($.the("i"), id),
                this.i);
//        FuzzyScalarConcepts in = senseNumberBi(
//                //$.inh($.the("i"), id),
//                $.p($.the("i"), id),
//                this.i);

//        action( new GoalActionConcept($.p($.the("o"), id), nar, (b,d) -> {
//            if (d!=null) {
//                o.setValue( .freq() + (-.5f)*2f*0.1f );
//            }
//        }));

        //out = null;
        out = actionTriState($.inh($.the("o"), id), (d) -> {
            float speed = 0.1f;
            switch (d) {
                case -1:
                case +1:
                    o.setValue(Math.max(0f, Math.min(1f, o.floatValue() + d * speed)));
                    break;
            }
        });

//        out = action(
//                //$.inh($.the("o"), id),
//                $.p($.the("o"), id),
//                (b, d) -> {
//
//            if (d != null) {
//                float f = d.freq();
////                float inc = ((f - 0.5f) * 2f) * 0.04f;
////                o.setValue(unitize(o.getValue() + inc));
//
//                o.setValue(f);
//
////                else {
////                    //no change
////                    return null;
////                }
//
//
//                return $.t(o.floatValue(), d.conf());
//            }
//
//            return null;
//
//
//        });

        in.resolution(resolution);


    }

    @Override
    protected float act() {
        if (o != o)
            return Float.NaN;

        float dist = Math.abs(i.floatValue() - o.floatValue());

        //return (1f - dist); //unipolar, 0..1.0
        //return (((1f - dist)-0.5f) * 2f); //bipolar, normalized to -1..+1
        return ((Util.sqr(1f - dist) - 0.5f) * 2f); //bipolar, normalized to -1..+1
    }


}
