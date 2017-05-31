package nars.test.agent;

import jcog.Util;
import jcog.data.FloatParam;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.ActionConcept;
import nars.concept.SensorConcept;
import nars.term.Compound;
import nars.term.atom.Atomic;


/**
 * 1 input x 1 output environment
 */
public class Line1DSimplest extends NAgent {

    //public final SensorConcept in;

    /**
     * the target value
     */
    public final FloatParam i = new FloatParam(0.5f, 0, 1f);
    public final FloatParam speed = new FloatParam(0.04f, 0f, 0.5f);

    public final ActionConcept out;
    /**
     * the current value
     */
    public final FloatParam o = new FloatParam(0.5f, 0, 1f);
    public final SensorConcept in;


    public Line1DSimplest(NAR n) {
        super("", n);

        in = senseNumber(
                //$.inh($.the("i"), id),
                //$.inh(Atomic.the("i"), id),
                $.p("i"),
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
        Compound O = //$.inh(Atomic.the("o"), id);
                $.p("o");
        out = actionTriState(O, (d) -> {
            switch (d) {
                case -1:
                case +1:
                    this.o.setValue(Math.max(0f, Math.min(1f, this.o.floatValue() + d * speed.floatValue())));
                    return true;

            }
            return true;
        });
//        out = actionBipolar(O, v -> {
//            float current = this.o.floatValue();
//            float nv = Util.unitize( current + v * speed.floatValue());
//                    //if (!Util.equals(nv, o.floatValue(), Param.TRUTH_EPSILON)) {
//                        o.setValue(
//                                nv
//                        );
//                        return true;
//                    //}
//                    //return false;
//                }
//        );

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


    }

    @Override
    protected float act() {
        float dist = Math.abs(in.asFloat() - o.floatValue());
        //dist = (float)(Math.sqrt(dist)); //more challenging

        //return (1f - dist); //unipolar, 0..1.0

        float r = (((1f - dist) - 0.5f) * 2f); //bipolar, normalized to -1..+1
        return r;
    }


    public float target() {
        return in.asFloat();
    }

    public void target(float v) {
        i.setValue(Util.unitize(v));
    }
}
