package nars.experiment.arkanoid;


import com.google.common.collect.Lists;
import nars.$;
import nars.NAR;
import nars.NARLoop;
import nars.agent.NAgent;
import nars.gui.BeliefTableChart;
import nars.gui.HistogramChart;
import nars.index.CaffeineIndex;
import nars.nar.Default;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.time.MySTMClustered;
import nars.term.Term;
import nars.term.Termed;
import nars.time.FrameClock;
import nars.util.Util;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.math.FloatNormalized;
import nars.util.signal.FuzzyScalar;
import nars.util.signal.MotorConcept;
import nars.video.CamView;
import nars.video.CameraSensor;
import nars.video.SwingCamera;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.ControlSurface;
import spacegraph.obj.GridSurface;
import spacegraph.obj.MatrixView;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import static java.util.stream.Collectors.toList;
import static nars.$.t;
import static nars.experiment.tetris.Tetris.DEFAULT_INDEX_WEIGHT;
import static nars.experiment.tetris.Tetris.exe;
import static spacegraph.obj.GridSurface.VERTICAL;

public class Arkancide extends NAgent {

    private static final int cyclesPerFrame = 3;
    public static final int runFrames = 5000;
    public static final int CONCEPTS_FIRE_PER_CYCLE = 16;

    /** decision frames */
    public static int BaseBrainwaveRate = 1;


    final int visW = 48;
    final int visH = 24;

    float paddleSpeed = 20f;




    final Arkanoid noid;

    private final CameraSensor pixels;

    private float prevScore;


    public Arkancide(NAR nar) {
        super(nar, BaseBrainwaveRate);

        noid = new Arkanoid();

        pixels = new CameraSensor(new SwingCamera(noid, visW, visH), this, (v) -> t(v, alpha));

        addSensor( new FuzzyScalar(new FloatNormalized(() -> (float)noid.paddle.x), nar,
                "pad(x,0)",
                "pad(x,1)",
                "pad(x,2)"
        ).resolution(0.05f) );

        addSensor( new FuzzyScalar(new FloatNormalized(() -> (float)noid.ball.x), nar,
                "ball(x,0)",
                "ball(x,1)",
                "ball(x,2)"
        ).resolution(0.05f) );

        addSensor( new FuzzyScalar(new FloatNormalized(() -> (float)noid.ball.y), nar,
                "ball(y)"
        ).resolution(0.05f) );


        addAction(new MotorConcept("(leftright)", nar, (b,d)->{
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
        GridSurface chart = newBeliefChart(narenv, window);
        new SpaceGraph().add(new Facial(chart).maximize()).show(800,600);
    }
    public static void newBeliefChartWindow(NAR nar, long window, Term... t) {
        GridSurface chart = newBeliefChart(nar, Lists.newArrayList(t), window);
        new SpaceGraph().add(new Facial(chart).maximize()).show(800,600);
    }
    public static void newBeliefChartWindow(NAR nar, long window, List<? extends Termed> t) {
        GridSurface chart = newBeliefChart(nar, t, window);
        new SpaceGraph().add(new Facial(chart).maximize()).show(800,600);
    }

    public static GridSurface newBeliefChart(NAgent narenv, long window) {
        NAR nar = narenv.nar;
        long[] btRange = new long[2];
        nar.onFrame(nn -> {
            long now = nn.time();
            btRange[0] = now - window;
            btRange[1] = now + window;
        });
        List<Surface> actionTables = narenv.actions.stream().map(c -> new BeliefTableChart(nar, c, btRange)).collect(toList());
        actionTables.add(new BeliefTableChart(nar, narenv.happy, btRange));
        actionTables.add(new BeliefTableChart(nar, narenv.joy, btRange));

        return new GridSurface(VERTICAL, actionTables);
    }

    public static GridSurface newBeliefChart(NAR nar, Collection<? extends Termed> narenv, long window) {
        long[] btRange = new long[2];
        nar.onFrame(nn -> {
            long now = nn.time();
            btRange[0] = now - window;
            btRange[1] = now + window;
        });
        List<Surface> actionTables = narenv.stream().map(c -> new BeliefTableChart(nar, c, btRange)).collect(toList());

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

        float p = 0.25f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.5f * p;
        nar.DEFAULT_GOAL_PRIORITY = 0.6f * p;
        nar.DEFAULT_QUESTION_PRIORITY = 0.4f * p;
        nar.DEFAULT_QUEST_PRIORITY = 0.4f * p;

        nar.cyclesPerFrame.set(cyclesPerFrame);
        nar.confMin.setValue(0.05f);
        nar.compoundVolumeMax.setValue(30);
        //nar.truthResolution.setValue(0.04f);

//        nar.on(new TransformConcept("seq", (c) -> {
//            if (c.size() != 3)
//                return null;
//            Term X = c.term(0);
//            Term Y = c.term(1);
//
//            Integer x = intOrNull(X);
//            Integer y = intOrNull(Y);
//            Term Z = (x!=null && y!=null)? ((Math.abs(x-y) <= 1) ? $.the("TRUE") : $.the("FALSE")) : c.term(2);
//
//
//            return $.inh($.p(X, Y, Z), $.oper("seq"));
//        }));
//        nar.believe("seq(#1,#2,TRUE)");
//        nar.believe("seq(#1,#2,FALSE)");

        //nar.log();
        //nar.logSummaryGT(System.out, 0.1f);

//		nar.log(System.err, v -> {
//			if (v instanceof Task) {
//				Task t = (Task)v;
//				if (t instanceof DerivedTask && t.punc() == '!')
//					return true;
//			}
//			return false;
//		});

        //Global.DEBUG = true;

        //new Abbreviation2(nar, "_");

        MySTMClustered stm = new MySTMClustered(nar, 128, '.', 4);
        MySTMClustered stmGoal = new MySTMClustered(nar, 128, '!', 3);

        //new ArithmeticInduction(nar);
        //new VariableCompressor(nar);



        Arkancide t = new Arkancide(nar);
        t.trace = true;


        //        view.autoenc = new MatrixView(ac.W.length, ac.W[0].length, arrayRenderer(ac.W));




        //view.attention.add(nar.inputActivation);
        //view.attention.add(nar.derivedActivation);

        newBeliefChartWindow(t, 200);
        HistogramChart.budgetChart(nar, 50);
        //BagChart.show((Default) nar);

        ControlSurface.newControlWindow(
                //new GridSurface(VERTICAL, actionTables),
                //BagChart.newBagChart((Default)nar, 1024),
                new CamView(t.pixels, nar)//, view
        );

        //newConceptWindow((Default) n, 64, 4);



        NARLoop loop = t.run(runFrames, 0);

        //Tetris2.NARController meta = new Tetris2.NARController(nar, loop, t);
        //newBeliefChart(meta, 500);

        loop.join();
        //t.run(runFrames, 0, 1).join();

        //nar.index.print(System.out);
        NAR.printTasks(nar, true);
        NAR.printTasks(nar, false);

        nar.printConceptStatistics();

    }


}