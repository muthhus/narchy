package nars.util.experiment;

import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.tuple.Tuples;
import nars.NAR;
import nars.concept.Concept;
import nars.learn.Agent;
import nars.learn.ql.DQN;
import nars.nar.Default;
import nars.op.time.MySTMClustered;
import nars.util.NAgent;
import nars.util.Optimize;
import nars.util.Texts;
import nars.util.data.Util;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.Arrays;

import static java.lang.System.out;

/**
 * Created by me on 5/4/16.
 */
public class Line1D implements Environment {


    public float targetPeriod = 20;
    int size = 8;
    int speed = 1;
    boolean print = true;
    private int yHidden;
    private int yEst;

    @Override public Twin<Integer> start() {

        //Global.DEBUG = true;


        yEst = size/2; //NAR estimate of Y
        yHidden = size/2; //actual best Y used by loss function


        return Tuples.twin(size, 2);
    }

    @Override
    public float cycle(int t, int aa, float[] ins, Agent a) {



        float dist =  ((float)Math.abs(yHidden - yEst)) / size;

        float closeness = 1f - dist;
        float reward = ((closeness*closeness) -0.5f)*2f;
        //float reward = dist < speed ? (0.5f/(1f+dist)) : -dist;
        //float reward = -dist + 0.1f;
        //float reward = 1f / (1+dist*dist);

        float de;
        switch (aa) {
            case 0:
                de = 1f*speed;
                break;
            case 1:
                de = -1f*speed;
                break;
//                case 3:
//                    de = 1f * speed/4f;
//                    break;
//                case 4:
//                    de = -1f * speed/4f;
//                    break;
            case 2:
            default:
                de = 0f; //nothing
                break;
        }

        yEst += de;

        if (yEst > size-1) yEst = size-1;
        if (yEst < 0) yEst = 0;

        yHidden = Math.round(function(t) * (size-1));
        yHidden = Math.max(0, yHidden);
        yHidden = Math.min(size-1, yHidden);

        //update perceived state:
        Arrays.fill(ins, 0.5f);
        ins[yHidden] += 0.5f;
        ins[yEst] -= 0.5f;


        if (print) {


            int colActual = yHidden;
            int colEst = yEst;
            for (int i = 0; i < size; i++) {

                char c;
                if (i == colActual)
                    c = 'X';
                else if (i == colEst)
                    c = '|';
                else
                    c = '.';

                out.print(c);
            }

            //out.print(Texts.n2(ins));

            //out.print(' ');
            //out.print(reward);
            out.print(' ');
            out.print(a.summary());
            out.println();
        }


        return reward;
    }

    public float function(int t) {
        float a = 0.95f;
        return 0.5f +
                0.5f * (a * (float)Math.sin(t / (targetPeriod))) +
                0.05f * (a * (float)Math.cos(t / (targetPeriod/3f))-1)
                ;
        //return 0.5f + 0.5f * (float)Math.tan(t / (targetPeriod)) + (float)Math.random()*0.1f;
    }

    public static void main(String[] args) {

        int cycles = 10000;
        Default nar = new Default();
        nar.beliefConfidence(0.1f);
        nar.DEFAULT_BELIEF_PRIORITY = 0.2f;
        nar.DEFAULT_GOAL_PRIORITY = 0.3f;
        nar.DEFAULT_QUESTION_PRIORITY = 0.3f;
        nar.DEFAULT_QUEST_PRIORITY = 0.2f;

        nar.cyclesPerFrame.setValue(64);

        new MySTMClustered(nar, 64, '.');

        new Line1D().run(
                new NAgent(nar),
                //new DQN(),
                cycles);

        System.out.println();
        NAR.printTasks(nar, true);
        NAR.printTasks(nar, false);
        nar.index.forEach(t -> {
            if (t instanceof Concept) {
                Concept c = (Concept)t;
                if (c.hasQuestions()) {
                    System.out.println(c.questions().iterator().next());
                }
            }
        });
    }


}
