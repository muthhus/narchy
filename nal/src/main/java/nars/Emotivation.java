package nars;

import jcog.Util;
import jcog.pri.Pri;
import nars.concept.Concept;
import nars.control.Cause;
import nars.task.ITask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * value-reinforceing emotion implementation
 * use:         n.setEmotion(new Emotivation(n));
 */
public class Emotivation extends Emotion {

    private final NAR nar;

    public Emotivation(NAR n) {
        super(n);
        this.nar = n;
    }

    /**
     * set the value of a cause trace
     */
    @Override
    public void value(short[] causes, float value) {

        if (Math.abs(value)<Pri.EPSILON) return; //no change

        int numCauses = causes.length;

        //float sum = 0.5f * numCauses * (numCauses + 1);
        float vPer = value / numCauses; //flat

        for (int i = 0; i < numCauses; i++) {
            short c = causes[i];
            Cause cc = nar.causes.get(c);
            if (cc == null)
                continue; //ignore, maybe some edge case where the cause hasnt been registered yet?
                /*assert(cc!=null): c + " missing from: " + n.causes.size() + " causes";*/

            //float vPer = (((float) (i + 1)) / sum) * value; //linear triangle increasing to inc, warning this does not integrate to 100% here
            if (vPer != 0) {
                cc.apply(vPer);
            }
        }
    }

    @Override
    public void onAnswer(Task question, @Nullable Task answer, float effectiveConf) {
        super.onAnswer(question, answer, effectiveConf);

        //reward answer for answering the question
        value(answer.cause(), effectiveConf);
    }

    /**
     * returns a "value" adjusted priority
     * which is also applied to the given task.
     * returns NaN possibly
     */
    public float evaluate(Task x) {

        float gain = nar.evaluate(x, x.cause(), nar.taskCauses.get(x));
        assert (gain == gain);
        if (gain != 0) {

            float amp = Util.tanhFast(gain) + 1f; //[0..+2]

//            amp *= amp; //sharpen, psuedo-logarithmic x^4
//            amp *= amp;

            return x.priMult(amp);
        } else {
            return x.pri();
        }
    }

    @Override
    public @Nullable ITask onInput(@NotNull ITask x) {


        if (x instanceof Task && !((Task) x).isCommand()) {
            Task t = (Task) x;

            value(t.cause(), Param.valueAtInput(t, nar));

            evaluate(t);
//            if (tp != tp || tp < Pri.EPSILON)
//                return null; //TODO track what might cause this

        }

        return x;
    }

    @Override
    public void onActivate(@NotNull Task t, float activation, Concept origin, NAR n) {
        super.onActivate(t, activation, origin, n);

        short[] x = t.cause();
        int xl = x.length;
        if (xl > 0) {
            float taskValue = origin.value(t, activation, n.time(), n);
            if (taskValue!=0)
                value(x, taskValue);
        }

    }

    public static float preferConfidentAndRelevant(@NotNull Task t, float activation, long when, NAR n) {
        return 0.001f * activation * (t.isBeliefOrGoal() ? t.conf(when, n.dur()) : 0.5f);
    }

}
