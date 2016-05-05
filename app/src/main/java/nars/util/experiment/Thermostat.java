package nars.util.experiment;

import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.tuple.Tuples;
import nars.nar.Default;
import nars.util.Agent;
import nars.util.DQN;
import nars.util.NAgent;
import nars.util.NAgentDebug;
import nars.util.data.Util;
import org.apache.commons.lang3.mutable.MutableFloat;

import static java.lang.System.out;
import static nars.util.NAgent.printTasks;
import static nars.util.Texts.n2;

/**
 * Created by me on 5/4/16.
 */
public class Thermostat implements Environment {


    public float targetPeriod = 220;
    public final float speed = 0.03f;
    boolean print = true;
    private MutableFloat yHidden;
    private MutableFloat yEst;

    @Override public Twin<Integer> start() {

        //Global.DEBUG = true;


        yEst = new MutableFloat(0.5f); //NAR estimate of Y
        yHidden = new MutableFloat(0.5f); //actual best Y used by loss function



        yEst.setValue(0.5f);


        return Tuples.twin(2, 3);
    }

    @Override
    public float cycle(int t, int aa, float[] ins, Agent a) {

        if (print) {

            int cols = 50;
            int colActual = Math.round(cols * yHidden.floatValue());
            int colEst = Math.round(cols * yEst.floatValue());
            for (int i = 0; i <= cols; i++) {

                char c;
                if (i == colActual)
                    c = 'X';
                else if (i == colEst)
                    c = '|';
                else
                    c = '.';

                out.print(c);
            }

            out.print(' ');
            out.print(a.summary());
            out.println();
        }


        float diff = yHidden.floatValue() - yEst.floatValue();
        //ins[0] = Util.clamp(diff);
        //ins[1] = Util.clamp(-diff);
        ins[0] = Util.clamp(yHidden.floatValue());
        ins[1] = Util.clamp(yEst.floatValue());

        float dist =  Math.abs(yHidden.floatValue() - yEst.floatValue());

        float reward = 1f-dist; reward *= reward;
        //float reward = dist < speed ? (0.5f/(1f+dist)) : -dist;
        //float reward = -dist + 0.1f;
        //float reward = 1f / (1+dist*dist);

        float de;
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

        yHidden.setValue( Util.clamp(function(t)) );

        return reward;

    }

    public float function(int t) {
        return 0.5f +
                0.5f * (float)Math.sin(t / (targetPeriod)) +
                0.05f * ((float)Math.cos(t / (targetPeriod/3f))-1)
                ;
        //return 0.5f + 0.5f * (float)Math.tan(t / (targetPeriod)) + (float)Math.random()*0.1f;
    }

    public static void main(String[] args) {
        Default n = new Default(256, 2, 1, 3);
        n.conceptActivation.setValue(0.5);
        n.cyclesPerFrame.set(64);
        //n.shortTermMemoryHistory.set(3);
        //n.logSummaryGT(System.out, 0.55f);

        //n.conceptRemembering.setValue(1);
        NAgent a = new NAgentDebug(n);

        new Thermostat().run(
            //new DQN(),
            a,
            12512
        );

        printTasks(n, true);
        printTasks(n, false);
    }


}
