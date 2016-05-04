package nars.util.experiment;

import nars.nar.Default;
import nars.util.Agent;
import nars.util.DQN;
import nars.util.NAgent;
import nars.util.data.Util;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import static java.lang.System.out;
import static nars.util.NAgent.printTasks;
import static nars.util.Texts.n2;

/**
 * Created by me on 5/4/16.
 */
public class Thermostat {


    public float targetPeriod = 120;
    public final float speed = 0.025f;
    boolean print = true;


    public float run(Agent a, int cycles) {

        final int inputs = 2;
        a.start(inputs, 3);


        //Global.DEBUG = true;


        MutableFloat yEst = new MutableFloat(0.5f); //NAR estimate of Y
        MutableFloat yHidden = new MutableFloat(0.5f); //actual best Y used by loss function

        float loss = 0;


        yEst.setValue(0.5f);

        float[] ins = new float[inputs];

        for (int t = 0; t < cycles; t++) {


            if (print) {

                int cols = 50;
                int colActual = Math.round(cols * yHidden.floatValue());
                int colEst = Math.round(cols * yEst.floatValue());
                for (int i = 0; i <= cols; i++) {

                    char c;
                    if (i == colActual)
                        c = '#';
                    else if (i == colEst)
                        c = '|';
                    else
                        c = '.';

                    out.print(c);
                }

                out.println();
            }


            float diff = yHidden.floatValue() - yEst.floatValue();
            ins[0] = Util.clamp(diff);
            ins[1] = Util.clamp(-diff);


            float estimated = yEst.floatValue();
            float actual = yHidden.floatValue();

            loss += (Math.abs(actual - estimated));




            float dist =  Math.abs(yHidden.floatValue() - yEst.floatValue());

            float reward = dist < speed ? (0.5f/(1f+dist)) : -dist;

            int aa = a.act(reward, ins);
            float de = 0;
            switch (aa) {
                case 1:
                    de = 1f * speed;
                    break;
                case 2:
                    de = -1f * speed;
                    break;
//                case 3:
//                    de = 1f * speed/4f;
//                    break;
//                case 4:
//                    de = -1f * speed/4f;
//                    break;
                case 0:
                default:
                    de = 0f; //nothing
                    break;
            }
            yEst.setValue( Util.clamp(yEst.floatValue() + de) );

            yHidden.setValue(0.5f + 0.5f * Math.sin(t / (targetPeriod)));


        }


        float lossAvg = loss/cycles;
        System.out.println(a + " loss=" + lossAvg);
        return lossAvg;
    }

    public static void main(String[] args) {
        Default n = new Default(256, 4, 2, 3);
        n.conceptActivation.setValue(0.25);
        n.cyclesPerFrame.set(64);
        //n.conceptRemembering.setValue(1);
        NAgent a = new NAgent(n);

        new Thermostat().run(
            //new DQN(),
            a,
            512
        );

        printTasks(n, true);
        printTasks(n, false);
    }


}
