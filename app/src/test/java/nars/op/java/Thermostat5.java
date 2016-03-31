package nars.op.java;

import nars.$;
import nars.nal.Tense;
import nars.nar.Default;
import nars.term.Compound;
import nars.util.data.Util;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import org.apache.commons.lang3.mutable.MutableFloat;

import static java.lang.System.out;


/**
 * Created by me on 3/31/16.
 */
public class Thermostat5 {

    public static final float basePeriod = 35;
    public static float targetPeriod = 1f;
    public static final float speed = 0.3f;

    public static void main(String[] args) {

        MutableFloat x0 = new MutableFloat();
        //MutableFloat x1 = new MutableFloat();
        MutableFloat yEst = new MutableFloat(0.5f); //NAR estimate of Y
        MutableFloat yHidden = new MutableFloat(); //actual best Y used by loss function

        Default n = new Default(1000, 10, 2,6);
        n.cyclesPerFrame.set(10);
        //n.derivationDurabilityThreshold.setValue(0.02f);
        //n.premiser.confMin.setValue(0.05f);

        n.onFrame(nn -> {

            //float switchPeriod = 20;
            //float highPeriod = 5f;

            x0.setValue( 0.5f + 0.5f * Math.cos(n.time()/(targetPeriod * basePeriod)) ); //high frequency phase
            //x1.setValue( 0.5f + 0.3f * Math.sin(n.time()/(highPeriod * period)) ); //low frequency phase

            //yHidden.setValue((n.time() / (switchPeriod * period)) % 2 == 0 ? x0.floatValue() : x1.floatValue());
            yHidden.setValue(x0);

            float actual = yHidden.floatValue();
            float estimated = yEst.floatValue();
            //out.println( actual + "," + estimated );


            int cols = 40;
            int colActual = (int)Math.round(cols * actual);
            int colEst = (int)Math.round(cols * estimated);
            for (int i = 0; i <= cols; i++) {

                char c;
                if (i == colActual)
                    c = 'a';
                else if (i == colEst)
                    c = 'e';
                else
                    c = '.';

                out.print(c);
            }

            out.println();
        });
        //n.on(new SensorConcept((Compound)$.$("a:x0"), n, ()-> x0.floatValue())
        //        .resolution(0.01f)/*.pri(0.2f)*/
        //);
        /*n.on(new SensorConcept((Compound)$.$("a:x1"), n, ()-> x1.floatValue())
                .resolution(0.01f).pri(0.2f)
        );*/
        n.on(new SensorConcept("diff:above", n, ()-> {
            float diff = yHidden.floatValue() - yEst.floatValue();
            if (diff > 0) return 0.5f + 0.5f * Util.clamp(diff);
            return 0;
        }).resolution(0.01f)/*.pri(0.2f)*/);
        n.on(new SensorConcept("diff:below", n, ()-> {
            float diff = -(yHidden.floatValue() - yEst.floatValue());
            if (diff > 0) return 0.5f + 0.5f * Util.clamp(diff);
            return 0;
        }).resolution(0.01f)/*.pri(0.2f)*/);

        n.on(new MotorConcept("t(up)", n, (v)->{
            yEst.setValue(Util.clamp(+speed * v + yEst.floatValue()));
            return v;
        }));
        n.on(new MotorConcept("t(down)", n, (v)->{
            yEst.setValue(Util.clamp(-speed* v + yEst.floatValue()));
            return v;
        }));




        n.logSummaryGT(System.out, 0.6f);
        n.goal($.$("t(up)"),  Tense.Present, 1f, 0.1f);
        n.goal($.$("t(up)"), Tense.Present, 0f, 0.1f);
        n.goal($.$("t(down)"), Tense.Present, 1f, 0.1f);
        n.goal($.$("t(down)"), Tense.Present, 0f, 0.1f);
        n.goal($.$("diff:above"), 0f, 0.99f); //not above
        n.goal($.$("diff:below"), 0f, 0.99f); //not below
        //n.ask($.$("(a:#x ==> diff:#y)"), '?'); //not above


        while (true) {

            //n.goal($.$("((--,diff:above) && (--,diff:below))"), Tense.Present, 1f, 0.99f); //not above or below

            n.step();
            Util.pause(5);
        }
    }
}
