package nars.util.experiment;

import com.gs.collections.api.block.function.primitive.FloatToObjectFunction;
import nars.Global;
import nars.NAR;
import nars.Narsese;
import nars.concept.table.BeliefTable;
import nars.nal.Tense;
import nars.nar.Default;
import nars.task.Task;
import nars.truth.DefaultTruth;
import nars.util.Agent;
import nars.util.DQN;
import nars.util.data.MutableInteger;
import nars.util.data.Util;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.TreeSet;

import static java.lang.System.out;
import static nars.util.Texts.n2;

/**
 * Created by me on 5/4/16.
 */
public class Thermostat {


    public final float tolerance = 0.03f;
    public float targetPeriod = 60;
    public final float speed = 0.15f;
    boolean print = true;


    public float score(Agent a, int cycles) {

        final int inputs = 2;
        a.start(inputs, 3);


        Global.DEBUG = true;


        MutableFloat yEst = new MutableFloat(0.5f); //NAR estimate of Y
        MutableFloat yHidden = new MutableFloat(0.5f); //actual best Y used by loss function

        MutableFloat loss = new MutableFloat(0);


        yEst.setValue(0.5f);

        float[] ins = new float[inputs];

        for (int t = 0; t < cycles; t++) {


            float diff = yHidden.floatValue() - yEst.floatValue();
            ins[0] = Util.clamp(diff);
            ins[1] = Util.clamp(-diff);


            float estimated = yEst.floatValue();


            float actual;

            yHidden.setValue(0.5f + 0.5f * Math.sin(t / (targetPeriod)));
            actual = yHidden.floatValue();

            loss.add(Math.abs(actual - estimated));


            if (print) {

                int cols = 50;
                int colActual = Math.round(cols * actual);
                int colEst = Math.round(cols * estimated);
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


            float dist =  Math.abs(yHidden.floatValue() - yEst.floatValue());

            float reward = dist < speed ? (0.5f/(1f+dist)) : -dist;

            int aa = a.act(reward, ins);
            float de = 0;
            switch (aa) {
                case 1:
                    de = 1f * speed/2f;
                    break;
                case 2:
                    de = -1f * speed/2f;
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



        }

        return loss.floatValue() / cycles;
    }

    public static void main(String[] args) {
        float score = new Thermostat().score( new DQN(), 5000 );
        System.out.println("score=" + score);
    }


}
