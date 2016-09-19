package nars.experiment.arkanoid;


import com.google.common.collect.Lists;
import nars.$;
import nars.NAR;
import nars.NARLoop;
import nars.NAgent;
import nars.gui.BagChart;
import nars.gui.BeliefTableChart;
import nars.gui.HistogramChart;
import nars.index.CaffeineIndex;
import nars.nar.Default;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.time.MySTMClustered;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.time.FrameClock;
import nars.util.Util;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.math.FloatNormalized;
import nars.concept.FuzzyScalarConcepts;
import nars.concept.MotorConcept;
import nars.util.signal.NObj;
import nars.video.CameraSensorView;
import nars.video.CameraSensor;
import nars.video.SwingCamera;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.GridSurface;
import spacegraph.obj.MatrixView;

import java.util.List;
import java.util.Random;

import static nars.$.t;
import static nars.experiment.tetris.Tetris.DEFAULT_INDEX_WEIGHT;
import static nars.experiment.tetris.Tetris.conceptLinePlot;
import static nars.experiment.tetris.Tetris.exe;
import static spacegraph.SpaceGraph.window;
import static spacegraph.obj.GridSurface.VERTICAL;
import static spacegraph.obj.GridSurface.col;
import static spacegraph.obj.GridSurface.grid;

public class Arkancide extends NAgent {

    private static final int cyclesPerFrame = 4;
    public static final int runFrames = 50000;
    public static final int CONCEPTS_FIRE_PER_CYCLE = 32;




    final int visW = 32;
    final int visH = 18;


    float paddleSpeed = 20f;




    final Arkanoid noid;

    private final CameraSensor pixels;

    private float prevScore;


    public Arkancide(NAR nar) {
        super(nar, 10 /* additional decision frames */);

        new NObj("noid", noid = new Arkanoid(), nar)
                .read("paddle.x", "ball.x", "ball.y", "ball.velocityX", "ball.velocityY")
                .in(this);

        pixels = new CameraSensor($.oper("noid"),
            new SwingCamera(noid, visW, visH), this, (v) -> t(v, alpha));



//        addSensor(this.padX = new FuzzyScalarConcepts(new FloatNormalized(() -> (float)noid.paddle.x), nar,
//                "pad(x,0)",
//                "pad(x,1)",
//                "pad(x,2)"
//        ).resolution(0.05f) );

//        addSensor( new FuzzyScalarConcepts(new FloatNormalized(() -> (float)noid.ball.x), nar,
//                "ball(x,0)",
//                "ball(x,1)",
//                "ball(x,2)"
//        ).resolution(0.05f) );
//
//        addSensor( new FuzzyScalarConcepts(new FloatNormalized(() -> (float)noid.ball.y), nar,
//                "ball(y)"
//        ).resolution(0.05f) );


        addAction(new MotorConcept(
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


    public class View {
        //public Surface camView;
        public List attention = $.newArrayList();
        public MatrixView autoenc;
    }





    public static void newBeliefChartWindow(NAgent narenv, long window) {
        GridSurface chart = BeliefTableChart.agentActions(narenv, window);
        new SpaceGraph().add(new Facial(chart).maximize()).show(800,600);
    }
    public static void newBeliefChartWindow(NAR nar, long window, Term... t) {
        GridSurface chart = agentActions(nar, Lists.newArrayList(t), window);
        new SpaceGraph().add(new Facial(chart).maximize()).show(800,600);
    }
    public static void newBeliefChartWindow(NAR nar, long window, List<? extends Termed> t) {
        GridSurface chart = agentActions(nar, t, window);
        new SpaceGraph().add(new Facial(chart).maximize()).show(800,600);
    }

    public static GridSurface agentActions(NAR nar, Iterable<? extends Termed> cc, long window) {
        long[] btRange = new long[2];
        nar.onFrame(nn -> {
            long now = nn.time();
            btRange[0] = now - window;
            btRange[1] = now + window;
        });
        List<Surface> actionTables = $.newArrayList();
        for (Termed c : cc) {
            actionTables.add( new BeliefTableChart(nar, c, btRange) );
        }

        return new GridSurface(VERTICAL, actionTables);
    }

    private static float noise(float v, float noiseLevel, Random rng) {
        if (noiseLevel > 0) {
            return Util.clamp(v + (rng.nextFloat() * noiseLevel));
        }
        return v;
    }

    @Override
    protected float act() {
        pixels.update();

        float nextScore = noid.next();
        float reward = nextScore - prevScore;
        this.prevScore = nextScore;
        return reward;
    }

    public static void main(String[] args) {
        Random rng = new XorShift128PlusRandom(1);

        //Multi nar = new Multi(3,512,
        Default nar = new Default(1024,
                CONCEPTS_FIRE_PER_CYCLE, 2, 2, rng,
                new CaffeineIndex(new DefaultConceptBuilder(rng), DEFAULT_INDEX_WEIGHT, false, exe)
                //new TreeIndex.L1TreeIndex(new DefaultConceptBuilder(new XORShiftRandom(3)), 100000, 8192, 2)

                , new FrameClock(), exe);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.7f);

        float p = 0.15f;
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

        Arkancide t = new Arkancide(nar);
        t.trace = true;


        int history = 2000;
        window(
            grid(
                new CameraSensorView(t.pixels, nar),
                BeliefTableChart.agentActions(t, history),
                BagChart.concepts(nar, 64),
                col(
                    HistogramChart.budgetChart(nar, 50),
                    conceptLinePlot(nar, t.actions, nar::conceptPriority, 200)
                )
        ), 500, 500);





        NARLoop loop = t.run(runFrames, 0);


        loop.join();

        NAR.printTasks(nar, true);
        NAR.printTasks(nar, false);

        nar.printConceptStatistics();

    }


}