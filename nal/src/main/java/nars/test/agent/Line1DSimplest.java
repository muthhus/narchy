package nars.test.agent;

import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.ActionConcept;
import nars.concept.SensorConcept;

import static nars.$.the;
import static nars.Op.BELIEF;


/**
 * 1 input x 1 output environment
 */
public class Line1DSimplest extends NAgent {


    public float target = 0.5f, current = 0.5f;

    public interface LongToFloatFunction {
        float valueOf(long i);
    }

    public final SensorConcept in;
    public final ActionConcept out;

    public Line1DSimplest(NAR n) {
        super("L", n);

        in = senseNumber($.func( the("in"), the("L")), () -> this.target );

        out = action($.func( the("out"), the("L")), (b, d) -> {

            float previous = current;

            if (d != null) {
                current = d.freq();
            }

            /*
            float prevDist = Math.abs(target - previous);
            float curDist = Math.abs(current - previous);
            if (curDist - prevDist > 0.01f) {
                System.err.println(nar.time() + " DEVIATION " + Texts.n2(curDist - prevDist));
            } else if (prevDist - curDist > 0.01f ){
                System.err.println(nar.time() + " CORRECTION " + Texts.n2(prevDist - curDist));
            }
            */

            return $.t(current, nar.confidenceDefault(BELIEF));
        });

    }

    @Override
    protected float act() {
        float dist = Math.abs(target - current);

        return ((1f - dist) * 2f) - 1f; //normalize to -1..+1
    }


    public static LongToFloatFunction sine(float targetPeriod) {
        return (t) -> 0.5f + 0.5f * (float) Math.sin(t / (targetPeriod));
        //+ 0.05f * (a * (float)Math.cos(t / (targetPeriod/3f))-1)
        //return 0.5f + 0.5f * (float)Math.tan(t / (targetPeriod)) + (float)Math.random()*0.1f;
    }

    public static LongToFloatFunction random(float targetPeriod) {
        return (t) -> (((((int) (t / targetPeriod)) * 31) ^ 37) % 256) / 256.0f;

        //+ 0.05f * (a * (float)Math.cos(t / (targetPeriod/3f))-1)
        //return 0.5f + 0.5f * (float)Math.tan(t / (targetPeriod)) + (float)Math.random()*0.1f;
    }





}
