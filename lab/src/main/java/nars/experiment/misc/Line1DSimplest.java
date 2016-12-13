package nars.experiment.misc;

import jcog.Util;
import jcog.data.random.XorShift128PlusRandom;
import nars.NAR;
import nars.NAgent;
import nars.Param;
import nars.concept.ActionConcept;
import nars.index.term.map.CaffeineIndex;
import nars.nar.Default;
import nars.nar.exe.Executioner;
import nars.nar.exe.SingleThreadExecutioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.time.FrameTime;

import static jcog.Texts.n2;
import static jcog.Util.unitize;


/**
 * Created by me on 5/4/16.
 */
public class Line1DSimplest extends NAgent {

    static {
        Param.DEBUG = true;
    }

    private float yHidden, yEst;

    public interface IntToFloatFunction {
        float valueOf(int i);
    }

    private final IntToFloatFunction targetFunc;
    final ActionConcept out;
    boolean print;


    public Line1DSimplest(NAR n, IntToFloatFunction target) {
        super("x", n, 16);

        this.targetFunc = target;

        this.trace = false;

        senseNumber("in(x)", ()->yHidden);

        //senseNumber(()->yHidden, "lo(x)", "hi(x)");

        out = action("out(x)", (b,d)->{
            if (d!=null)
                yEst = d.freq();
            return d;
        });


    }

    @Override
    protected float act() {

        yHidden = unitize(
                Math.round(
                    targetFunc.valueOf((int) now)
                )
        );

        float dist =  Math.abs(yHidden - yEst);

        //float closeness = 1f - dist;
        //float reward = ((closeness*closeness*closeness) -0.5f)*2f;
        //float reward = dist < speed ? (0.5f/(1f+dist)) : -dist;
        float reward =
                -dist * 2f + 1f;
                //(1f-dist)*(1f-dist);
                //1f / (1+dist*dist);

        //reward = Util.sqr(reward); //sharpen


        if (print) {
            String a = n2(yHidden) + ",  " + n2(yEst);
            System.out.println(a
                    //+ "\t" + summary());
                    );

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

        float dur = 1;

        final Executioner exe =
                //new MultiThreadExecutioner(2, 2048);
                new SingleThreadExecutioner();

        Default nar = new Default(512,
                64, 1, 3, rng,
                new CaffeineIndex(new DefaultConceptBuilder(), 1024*2, 12, false, exe),
                new FrameTime(dur), exe
        );
        nar.termVolumeMax.set(9);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.9f);

        //nar.truthResolution.setValue(0.02f);


        Line1DSimplest l = new Line1DSimplest(nar,
                sine(32)
                //random(256)
        );



        //NAgents.chart(l);

        nar.logSummaryGT(System.out, 0.01f);

//        nar.onTask(t -> {
//            if (t instanceof DerivedTask && t.isGoal())
//                System.out.println(t.proof());
//        });


        l.print = true;
        //l.runRT(25, 15000).join();
        l.run(1024);


        NAR.printActiveTasks(nar, true);
        NAR.printActiveTasks(nar, false);

        l.actions.forEach(a -> a.print());
//
//        l.predictors.forEach(p->{
//           nar.concept(p).print();
//        });
        System.out.println("AVG SCORE=" + l.rewardSum()/ nar.time());

    }

}
