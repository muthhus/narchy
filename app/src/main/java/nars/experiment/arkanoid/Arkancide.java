package nars.experiment.arkanoid;


import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import nars.NAR;
import nars.NARLoop;
import nars.gui.Vis;
import nars.index.CaffeineIndex;
import nars.nar.Default;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.time.MySTMClustered;
import nars.remote.SwingAgent;
import nars.time.FrameClock;
import nars.util.data.random.XorShift128PlusRandom;
import nars.concept.ActionConcept;
import nars.util.signal.NObj;
import nars.video.CameraSensorView;
import spacegraph.Surface;

import java.util.Random;
import java.util.function.Function;

import static nars.$.t;
import static nars.experiment.tetris.Tetris.DEFAULT_INDEX_WEIGHT;
import static nars.experiment.tetris.Tetris.conceptLinePlot;
import static nars.experiment.tetris.Tetris.exe;
import static spacegraph.SpaceGraph.window;
import static spacegraph.obj.GridSurface.col;
import static spacegraph.obj.GridSurface.grid;

public class Arkancide extends SwingAgent {

    private static final int cyclesPerFrame = 4;
    public static final int runFrames = 50000;
    public static final int CONCEPTS_FIRE_PER_CYCLE = 32;




    final int visW = 32;
    final int visH = 18;


    float paddleSpeed = 20f;


    final Arkanoid noid;

    private float prevScore;


    public Arkancide(NAR nar) {
        super(nar, 10 /* additional decision frames */);

        new NObj("noid", noid = new Arkanoid(), nar)
                .read("paddle.x", "ball.x", "ball.y", "ball.velocityX", "ball.velocityY")
                .into(this);

        addCamera("noid", noid, visW, visH);


        addAction(new ActionConcept(
                //"happy:noid(paddle,x)"
                "(leftright)"
                , nar, (b,d)->{
            if (d!=null) {
                //TODO add limits for feedback, dont just return the value
                //do this with a re-usable feedback interface because this kind of acton -> limitation detection will be common
                noid.paddle.move((d.freq() - 0.5f) * paddleSpeed);
                return d.withConf(alpha);
            } else {
                return null;
            }
        }));

//        AutoClassifier ac = new AutoClassifier($.the("row"), nar, sensors,
//                4, 8 /* states */,
//                0.05f);

    }

    @Override
    protected float reward() {
        float nextScore = noid.next();
        float reward = nextScore - prevScore;
        this.prevScore = nextScore;
        return reward;
    }

    public static void main(String[] args) {
        playSwing(Arkancide::new);
    }

    public static void playSwing(Function<NAR, SwingAgent> init) {
        Random rng = new XorShift128PlusRandom(1);

        //Multi nar = new Multi(3,512,
        Default nar = new Default(1024,
                CONCEPTS_FIRE_PER_CYCLE, 2, 2, rng,
                new CaffeineIndex(new DefaultConceptBuilder(rng), DEFAULT_INDEX_WEIGHT, false, exe)
                //new TreeIndex.L1TreeIndex(new DefaultConceptBuilder(new XORShiftRandom(3)), 100000, 8192, 2)

                , new FrameClock(), exe);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.7f);

        float p = 0.1f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.75f * p;
        nar.DEFAULT_GOAL_PRIORITY = 1f * p;
        nar.DEFAULT_QUESTION_PRIORITY = 0.25f * p;
        nar.DEFAULT_QUEST_PRIORITY = 0.5f * p;

        nar.cyclesPerFrame.set(cyclesPerFrame);
        nar.confMin.setValue(0.02f);
        nar.compoundVolumeMax.setValue(32);
        //new Abbreviation2(nar, "_");

        MySTMClustered stm = new MySTMClustered(nar, 128, '.', 3);
        MySTMClustered stmGoal = new MySTMClustered(nar, 128, '!',2);

        SwingAgent a = init.apply(nar);
        a.trace = true;


        int history = 2000;
        window(
            grid(
                grid( a.widgets.values().stream().map(cs -> new CameraSensorView(cs, nar)).toArray(Surface[]::new) ),
                Vis.agentActions(a, history),
                Vis.concepts(nar, 32),
                col(
                    Vis.budgetHistogram(nar, 25),
                    conceptLinePlot(nar,
                            Iterables.concat(a.actions, Lists.newArrayList(a.happy, a.joy)),
                            nar::conceptPriority, 200)
                )
        ), 900, 900);


        NARLoop loop = a.run(runFrames, 0);
        loop.join();

        NAR.printTasks(nar, true);
        NAR.printTasks(nar, false);
        nar.printConceptStatistics();
    }


}