package nars.experiment.misc;

import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.ActionConcept;
import nars.concept.SensorConcept;
import nars.index.CaffeineIndex;
import nars.nar.Default;
import nars.nar.exe.Executioner;
import nars.nar.exe.SingleThreadExecutioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.term.Term;
import nars.time.FrameClock;
import nars.util.data.random.XorShift128PlusRandom;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static java.lang.System.out;


/**
 * Created by me on 5/4/16.
 */
public class Line1DContinuous extends NAgent {

    public interface IntToFloatFunction {
        float valueOf(int i);
    }

    private final IntToFloatFunction targetFunc;
    int size;
    boolean print;
    private float yHidden;
    private float yEst;
    float speed = 1f;
    final float[] ins;

    public Line1DContinuous(NAR n, int size, IntToFloatFunction target) {
        super(n);
        this.size = size;
        ins = new float[size*2];
        this.targetFunc = target;

        yEst = size/2; //NAR estimate of Y
        yHidden = size/2; //actual best Y used by loss function


        for (int i = 0; i < size; i++) {
            int ii = i;
            //hidden
            @NotNull Term actual = $.the(0 /*"h"*/);
            sensors.add(new SensorConcept($.p(actual, $.the(i)), n, ()->{
                return ins[ii];
            }, (v) -> $.t(v, alpha)));

            //estimated
            @NotNull Term estim = $.the(1 /*"e"*/);
            sensors.add(new SensorConcept($.p(estim, $.the(i)), n, ()->{
                return ins[size + ii];
            }, (v) -> $.t(v, alpha)));
        }

        actions.add(new ActionConcept("(leftright)", n, (b, d) -> {
            if (d!=null) {
                float v =
                        //d.expectation();
                        d.freq();
                yEst += (v -0.5f)*speed;
            }
            return d;
        }));

        trace = false;

    }

    @Override
    protected float act() {


        yHidden = Math.round(targetFunc.valueOf((int) now) * (size-1));

        yHidden = Math.min(size-1, Math.max(0, yHidden));
        yEst    = Math.min(size-1, Math.max(0, yEst));



        //update perceived state:
        //1*size
        //        Arrays.fill(ins, 0.5f);
        //        ins[yHidden] += 0.5f;
        //        ins[yEst] -= 0.5f;
        //2*size
        Arrays.fill(ins, 0f);
        ins[Math.round(yHidden)] = 1f;
        ins[Math.round(size + yEst)] = 1f;


        float dist =  Math.abs(yHidden - yEst) / size;

        //float closeness = 1f - dist;
        //float reward = ((closeness*closeness*closeness) -0.5f)*2f;
        //float reward = dist < speed ? (0.5f/(1f+dist)) : -dist;
        float reward = -dist * 2f + 1f;
        //float reward = 1f / (1+dist*dist);



//        float de;
//        switch (aa) {
//            case 1: //right
//                de = 1f*speed;
//                break;
//            case 0: //left
//                de = -1f*speed;
//                break;
////                case 3:
////                    de = 1f * speed/4f;
////                    break;
////                case 4:
////                    de = -1f * speed/4f;
////                    break;
//            case 2:
//            default:
//                de = 0f; //nothing
//                break;
//        }




        if (yEst > size-1) yEst = size-1;
        if (yEst < 0) yEst = 0;


        if (print) {


            int colActual = Math.round(yHidden);
            int colEst = Math.round(yEst);
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
            out.print(summary());
            out.println();
        }

        return reward;
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

        XorShift128PlusRandom rng = new XorShift128PlusRandom((int)(Math.random()*1000));
        int cyclesPerFrame = 8;
        int conceptsPerCycle = 16;

        final Executioner exe =
                //new MultiThreadExecutioner(2, 2048);
                new SingleThreadExecutioner();

        Default nar = new Default(1024,
                conceptsPerCycle, 2, 2, rng,
                new CaffeineIndex(new DefaultConceptBuilder(rng), 1024*16, 12, false, exe),
                new FrameClock(), exe
        );

        nar.cyclesPerFrame.setValue(cyclesPerFrame);

        nar.beliefConfidence(0.95f);
        nar.goalConfidence(0.7f);
        nar.DEFAULT_BELIEF_PRIORITY = 0.02f;
        nar.DEFAULT_GOAL_PRIORITY = 0.02f;
        nar.DEFAULT_QUESTION_PRIORITY = 0.01f;
        nar.DEFAULT_QUEST_PRIORITY = 0.01f;

        nar.compoundVolumeMax.set(16);

        Line1DContinuous l = new Line1DContinuous(nar, 6,
                sine(55)
                //random(120)
        );
        l.print = true;
        l.runSync(1500);

        NAR.printTasks(nar, true);
        NAR.printTasks(nar, false);
        System.out.println("AVG SCORE=" + l.rewardSum()/ nar.time());
    }

}
