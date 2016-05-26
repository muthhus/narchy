package nars.util.experiment;

import com.gs.collections.api.block.function.primitive.IntToFloatFunction;
import com.gs.collections.api.block.function.primitive.IntToIntFunction;
import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.tuple.Tuples;
import nars.NAR;
import nars.concept.Concept;
import nars.learn.Agent;
import nars.learn.ql.DQN;
import nars.learn.ql.HaiQAgent;
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


    private final IntToFloatFunction targetFunc;
    int size;
    int speed = 1;
    boolean print = true;
    private int yHidden;
    private int yEst;

    public Line1D(int size, IntToFloatFunction target) {
        this.size = size;
        this.targetFunc = target;
    }

    @Override public Twin<Integer> start() {

        //Global.DEBUG = true;


        yEst = size/2; //NAR estimate of Y
        yHidden = size/2; //actual best Y used by loss function


        return Tuples.twin(size*2, 3);
    }

    @Override
    public float pre(int t, float[] ins) {


        yHidden = Math.round(targetFunc.valueOf(t) * (size-1));
        yHidden = Math.max(0, yHidden);
        yHidden = Math.min(size-1, yHidden);


        //update perceived state:
        {
            //1*size
            //        Arrays.fill(ins, 0.5f);
            //        ins[yHidden] += 0.5f;
            //        ins[yEst] -= 0.5f;
        }
        {
            //2*size
            Arrays.fill(ins, 0f);
            ins[yHidden] = 1f;
            ins[size + yEst] = 1f;
        }



        float dist =  ((float)Math.abs(yHidden - yEst)) / size;

        float closeness = 1f - dist;
        float reward = ((closeness*closeness*closeness) -0.5f)*2f;
        //float reward = dist < speed ? (0.5f/(1f+dist)) : -dist;
        //float reward = -dist + 0.1f;
        //float reward = 1f / (1+dist*dist);


        return reward;
    }

    @Override
    public void post(int t, int aa, float[] ins, Agent a) {


        float de;
        switch (aa) {
            case 1: //right
                de = 1f*speed;
                break;
            case 0: //left
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




        if (print) {


            int colActual = yHidden;
            int colEst = yEst;
            for (int i = 0; i < size; i++) {

                char c;
                if (i == colActual && i == colEst) {
                    c = '@';
                }else if (i == colActual)
                    c = 'X';
                else if (i == colEst)
                    c = '+';
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


    }


    public static IntToFloatFunction sine(float targetPeriod) {
        return (t) -> 0.5f + 0.5f * (float) Math.sin(t / (targetPeriod));
        //+ 0.05f * (a * (float)Math.cos(t / (targetPeriod/3f))-1)
        //return 0.5f + 0.5f * (float)Math.tan(t / (targetPeriod)) + (float)Math.random()*0.1f;
    }
    public static IntToFloatFunction random(float targetPeriod) {
        return (t) -> (((((int)(t/targetPeriod)) * 31) ^ 37) % 256)/256.0f;

        //+ 0.05f * (a * (float)Math.cos(t / (targetPeriod/3f))-1)
        //return 0.5f + 0.5f * (float)Math.tan(t / (targetPeriod)) + (float)Math.random()*0.1f;
    }

    public static void main(String[] args) {


        Default nar = new Default(1024, 8, 1, 3);

        nar.beliefConfidence(0.25f);
        nar.DEFAULT_BELIEF_PRIORITY = 0.1f;
        nar.DEFAULT_GOAL_PRIORITY = 0.4f;
        nar.DEFAULT_QUESTION_PRIORITY = 0.3f;
        nar.DEFAULT_QUEST_PRIORITY = 0.2f;

        nar.cyclesPerFrame.setValue(64);

        //nar.log();

        new MySTMClustered(nar, 64, '.');
        int cycles = 10000;
        float score = new Line1D(8,
                //random(10)
                sine(10)
        ).run(
                new NAgent(nar),
                //new DQN(),
                //new HaiQAgent(),
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

        System.err.println(score);

    }


}
