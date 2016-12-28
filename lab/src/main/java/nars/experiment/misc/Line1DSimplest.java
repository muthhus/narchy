package nars.experiment.misc;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.Param;
import nars.concept.ActionConcept;
import nars.nar.Default;
import nars.time.FrameTime;

import static jcog.Texts.n2;
import static jcog.Util.unitize;


/**
 * Created by me on 5/4/16.
 */
public class Line1DSimplest extends NAgent {

    static {
        Param.DEBUG = false;
    }

    private float yHidden = 0.5f, yEst = 0.5f;

    public interface IntToFloatFunction {
        float valueOf(int i);
    }

    private final IntToFloatFunction targetFunc;
    final ActionConcept out;
    boolean print;


    public Line1DSimplest(NAR n, IntToFloatFunction target) {
        super("", n, 1);

        this.targetFunc = target;

        this.trace = false;

        senseNumber("(in)", () -> yHidden);

        //senseNumber(()->yHidden, "lo(x)", "hi(x)");

        out = action("(out)", (b, d) -> {
            if (d != null) {
                yEst = unitize(
                        Util.lerp(d.freq(), yEst, 0.5f + 0.5f * d.conf())
                        //d.freq()
                );
            }
            return $.t(yEst, nar.confidenceDefault('.'));
        });


    }

    @Override
    protected float act() {

        yHidden = unitize(
                //Math.round(
                targetFunc.valueOf((int) now)
                //)
        );

        float dist = Math.abs(yHidden - yEst);

        //float closeness = 1f - dist;
        //float reward = ((closeness*closeness*closeness) -0.5f)*2f;
        //float reward = dist < speed ? (0.5f/(1f+dist)) : -dist;
        float reward =
                1f - dist;
        //(1f-dist)*(1f-dist);
        //1f / (1+dist*dist);

        //reward = Util.sqr(reward); //sharpen
        reward = reward * 2f - 1f; //normalize to -1..+1

        if (print) {
            String a = "rew=" + reward + "(dist=" + dist + "), tgt=" + n2(yHidden) + ",  cur=" + n2(yEst);
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
        return (t) -> (((((int) (t / targetPeriod)) * 31) ^ 37) % 256) / 256.0f;

        //+ 0.05f * (a * (float)Math.cos(t / (targetPeriod/3f))-1)
        //return 0.5f + 0.5f * (float)Math.tan(t / (targetPeriod)) + (float)Math.random()*0.1f;
    }



    public static void main(String[] args) {

        //XorShift128PlusRandom rng = new XorShift128PlusRandom((int) (Math.random() * 1000));

        float dur = 8;

        /*NAR nar = mock(Default.class, withSettings()
                        .useConstructor()
                        //.name("nar")
                        .defaultAnswer(CALLS_REAL_METHODS)
                        //.invocationListeners(new InvocationLogger())
        );*/

        NAR nar = new Default();



//        final Executioner exe =
//                //new MultiThreadExecutioner(2, 2048);
//                new SingleThreadExecutioner();
//        Default nar = new Default(1024,
//                4, 1, 3, rng,
//                new CaffeineIndex(new DefaultConceptBuilder(), 1024 * 16, 12, false, exe),
//                new FrameTime(dur), exe
//        );
        nar.termVolumeMax.set(13);


//        nar.DEFAULT_BELIEF_PRIORITY = 0.5f;
//        nar.DEFAULT_GOAL_PRIORITY = 0.5f;
        nar.beliefConfidence(0.5f);
        nar.goalConfidence(0.5f);

        //nar.quaMin.setValue(0.5f);

        //nar.truthResolution.setValue(0.02f);


        Line1DSimplest l = new Line1DSimplest(nar,
                //sine(16 * dur)
                random(64 * dur)
        );


        //l.epsilonProbability.setValue( 0f );

        //NAgents.chart(l);

        //nar.log();

        //nar.logSummaryGT(System.out, 0.0f);

//        //1. in > out: increase out
//        String inMinOut = "((x)-->(in-out))";
//        nar.input("$0.99;0.99$ (" + inMinOut + " ==>+0 out(x)). %1.00;0.7%");
//
//        //2. out > in: decrease out
//        String outMinIn = "((x)-->(out-in))";  //"--" + inMinOut; //reverse
//        nar.input("$0.99;0.99$ (" + outMinIn + " ==>+0 out(x)). %0.00;0.7%");
//
//        //3. out==in: stable
//        nar.input("$0.99;0.99$ ( --((x)-->(in-out)) ==>+0 out(x)). %0.50;0.8%");

//        nar.onCycle(nn -> {
////            Concept b = nar.concept("out(x)");
////            if (b != null)
////                System.out.println(b + " belief=" + b.belief(nn.time()));
//
//            Concept c = nar.concept(inMinOut);
//            if (c != null)
//                System.out.println(c + " belief=" + c.belief(nn.time()));
//
//            Concept d = nar.concept(outMinIn);
//            if (d != null)
//                System.out.println(d + " belief=" + d.belief(nn.time()));
//
//        });

//        nar.onTask(t -> {
//            if (t instanceof DerivedTask && t.isGoal())
//                System.out.println(t.proof());
//        });


        l.print = true;
        //l.runRT(25, 15000).join();


        l.runRT(25f, 1000).join();
        nar.stop();

//        NAR.printActiveTasks(nar, true);
//        NAR.printActiveTasks(nar, false);

        //l.actions.forEach(a -> a.print());

//        nar.concepts.forEach(x -> {
//            if (x.op()==IMPL) {
//                Concept c = (Concept)x;
//                System.out.println(c.toString());
//            }
//        });
//
//        l.predictors.forEach(p->{
//           nar.concept(p).print();
//        });
        System.out.println("AVG SCORE=" + l.rewardSum() / nar.time());

    }

}
