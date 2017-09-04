package nars.test.agent;

import jcog.Util;
import jcog.data.FloatParam;
import jcog.math.FloatNormalized;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.GoalActionConcept;
import nars.concept.SensorConcept;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;


/**
 * 1 input x 1 output environment
 */
public class Line1DSimplest extends NAgent {

    //public final SensorConcept in;

    /**
     * the target value
     */
    public final FloatParam i = new FloatParam(0f, 0, 1f);
    public final FloatParam speed = new FloatParam(0.04f, 0f, 0.5f);

    @NotNull
    public final GoalActionConcept up, down;

    /**
     * the current value
     */
    public final FloatParam o = new FloatParam(0.5f, 0, 1f);
    public final SensorConcept in;


    public Line1DSimplest(NAR n) {
        super("", n);


        in = senseNumber($.p("i"),                //$.inh($.the("i"), id),                 //$.inh(Atomic.the("i"), id),
                this.i
        );
//        in = senseNumber(
//                //$.inh($.the("i"), id),
//                $.the("i"),
//                this.i, 2, ScalarConcepts.Needle);

//        action( new GoalActionConcept($.p($.the("o"), id), nar, (b,d) -> {
//            if (d!=null) {
//                o.setValue( .freq() + (-.5f)*2f*0.1f );
//            }
//        }));

        float[] x = new float[2];
        onFrame(()->{
            float d = x[0] - x[1];
            this.o.setValue(Util.unitize(o.floatValue() + d*speed.floatValue()));
        });

        up = actionUnipolar($.p("up"), d -> {
            //if (d < 0.5f)  return 0; d -= 0.5f; d *= 2f;
            synchronized (o) {
                //float prev = o.floatValue();
                //float sp = speed.floatValue();
                //float next = Math.min(1, prev + d * sp);
//                if (!Util.equals(prev, next, sp )) {

                x[0] = d - 0.5f;
                    //this.o.setValue(next);
                    return d;
//                } else {
//                    return 0f;
//                }
            }
        });
        down = actionUnipolar($.p("down"), d -> {
            //if (d < 0.5f)  return 0; d -= 0.5f; d *= 2f;
            synchronized (o) {
                //float prev = o.floatValue();
                //float sp = speed.floatValue();
                //float next = Math.max(0, prev - d * sp);
                //if (!Util.equals(prev, next, sp )) {
                    //this.o.setValue(next);
                    x[1] = d - 0.5f;
                    return d;
//                } else {
//                    return 0f;
//                }
            }
        });

//        out = actionUnipolar(O, (d) -> {
//            this.o.setValue(d);
//            return d;
//        });
//        out = actionTriState(O, (d) -> {
//            switch (d) {
//                case -1:
//                case +1:
//                    this.o.setValue(Math.max(0, Math.min(1f, this.o.floatValue() + d * speed.floatValue())));
//                    break;
//            }
//        });
//        out = actionBipolar(O, v -> {
//            //float current = this.o.floatValue();
//
//            //if (!Util.equals(nv, o.floatValue(), Param.TRUTH_EPSILON)) {
//            if (v == v) {
//                o.setValue(
//                        //Util.unitize( current + v * speed.floatValue())
//                        v
//                );
//            }
//            return o.floatValue();
//            //}
//            //return false;
//        });

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
        float dist = Math.abs(
                i.floatValue() -
                        o.floatValue()
        );

        //dist = (float)(Math.sqrt(dist)); //more challenging

        return (1f - dist) * 2 - 1; //unipolar, 0..1.0


//        float r = (1f - dist/2f); //bipolar, normalized to -1..+1
//        if (r < 0.25f) return 1f;
//        return -1f;

        //return Util.sqr(r);
        //return (r-1f);
    }


    public float target() {
        return i.asFloat();
    }

    public void target(float v) {
        i.setValue(v);
    }
}
