package nars.experiment.arkanoid;


import com.google.common.collect.Lists;
import nars.$;
import nars.NAR;
import nars.NARLoop;
import nars.data.AutoClassifier;
import nars.gui.BeliefTableChart;
import nars.gui.HistogramChart;
import nars.index.TreeIndex;
import nars.nar.Default;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.NAgent;
import nars.op.VariableCompressor;
import nars.op.time.MySTMClustered;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.time.FrameClock;
import nars.truth.Truth;
import nars.util.Util;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import nars.vision.SwingCamera;
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
import static nars.experiment.tetris.Tetris.arrayRenderer;
import static nars.experiment.tetris.Tetris.exe;
import static nars.vision.PixelCamera.decodeRed;
import static spacegraph.obj.GridSurface.VERTICAL;

public class Arkancide extends NAgent {

    private static final int cyclesPerFrame = 1;
    public static final int runFrames = 50000;
    public static final int CONCEPTS_FIRE_PER_CYCLE = 128;
    public static final int INDEX_SIZE = 4 * 10000000;
    final Arkanoid noid;
    private SwingCamera cam;

    private MotorConcept motorLeftRight;

    final int visW = 30;
    final int visH = 14;
    SensorConcept[][] ss;

    //private final int visionSyncPeriod = 16;
    float noiseLevel;

    float paddleSpeed = 20f;
    private float prevScore;

    public class View {
        //public Surface camView;
        public List attention = $.newArrayList();
        public MatrixView autoenc;
    }
    private final View view = new View();

    public Arkancide(NAR nar) {
        super(nar);
        noid = new Arkanoid();
        ss = new SensorConcept[visW][visH];
    }

    @Override
    protected void init(NAR n) {


        cam = new SwingCamera(noid, visW, visH);
        cam.update();

        for (int x = 0; x < visW; x++) {
            int xx = x;
            for (int y = 0; y < visH; y++) {
                Compound squareTerm = $.p(x, y);
                int yy = y;
                SensorConcept sss;
                sensors.add(sss = new SensorConcept(squareTerm, nar,
                        () -> noise(decodeRed(cam.out.getRGB(xx, yy))) ,// > 0.5f ? 1 : 0,
                        (v) -> t(v, alpha)
                ));
                //sss.sensor.dur = 0.1f;
                //sss.timing(0,visionSyncPeriod);
                ss[x][y] = sss;
            }
        }



        AutoClassifier ac = new AutoClassifier($.the("row"), nar, sensors,
                4, 8 /* states */,
                0.05f);
        view.autoenc = new MatrixView(ac.W.length, ac.W[0].length, arrayRenderer(ac.W));

        MatrixView.ViewFunc camViewView = (x, y, g) -> {
//            int rgb = cam.out.getRGB(x,y);
//            float r = decodeRed(rgb);
//            if (r > 0)
//                System.out.println(x + " "+ y + " " + r);
//            g.glColor3f(r,0,0);

            SensorConcept s = ss[x][y];
            Truth b =  s.hasBeliefs() ? s.beliefs().truth(now) : null;
            float bf = b!=null ? b.freq() : 0.5f;
            Truth d = s.hasGoals() ? s.goals().truth(now) : null;
            float dr, dg;
            if (d == null) {
                dr = dg = 0;
            } else {
                float f = d.freq();
                float c = d.conf();
                if (f > 0.5f) {
                    dr = 0;
                    dg = (f - 0.5f) * 2f;// * c;
                } else {
                    dg = 0;
                    dr = (0.5f - f) * 2f;// * c;
                }
            }

            float maxConceptPriority = ((Default)nar).core.concepts.priMax(); //TODO cache this
            float p = nar.conceptPriority(s);
            p /= maxConceptPriority;
            g.glColor4f(dr, dg, bf, 0.5f + 0.5f * p);

            return ((b!=null ? b.conf() : 0) + (d!=null ? d.conf() : 0))/4f;

        };

        MatrixView camView = new MatrixView(visW, visH,
                camViewView);


        actions.add(motorLeftRight = new MotorConcept("(leftright)", nar, (b,d)->{

            if (d!=null) {
                noid.paddle.move((d.freq() - 0.5f) * paddleSpeed);
                return d.withConf(alpha);
            }
            return null;

            //return $.t((float)(noid.paddle.x / noid.SCREEN_WIDTH), 0.9f);

            //@Nullable Truth tNow = motorLeftRight.goals().truth(now);
            //if (tNow!=null)
                //noid.paddle.set(tNow.freq());

            //return $.t(noid.paddle.moveTo(d.freq(), paddleSpeed), 0.9f);
        }));

        //view.attention.add(nar.inputActivation);
        //view.attention.add(nar.derivedActivation);

        newBeliefChartWindow(this, 400);
        HistogramChart.budgetChart(nar, 50);

        ControlSurface.newControlWindow(
                //new GridSurface(VERTICAL, actionTables),
                //BagChart.newBagChart((Default)nar, 1024),
                camView//, view
        );

        //newConceptWindow((Default) n, 64, 4);


    }

    public static void newBeliefChartWindow(NAgent narenv, long window) {
        GridSurface chart = newBeliefChart(narenv, window);
        new SpaceGraph().add(new Facial(chart).maximize()).show(800,600);
    }
    public static void newBeliefChartWindow(NAR nar, long window, Term... t) {
        GridSurface chart = newBeliefChart(nar, Lists.newArrayList(t), window);
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
    public static GridSurface newBeliefChart(NAR nar, Collection<Termed> narenv, long window) {
        long[] btRange = new long[2];
        nar.onFrame(nn -> {
            long now = nn.time();
            btRange[0] = now - window;
            btRange[1] = now + window;
        });
        List<Surface> actionTables = narenv.stream().map(c -> new BeliefTableChart(nar, c, btRange)).collect(toList());

        return new GridSurface(VERTICAL, actionTables);
    }
    private float noise(float v) {
        if (noiseLevel > 0) {
            return Util.clamp(v + (nar.random.nextFloat() * noiseLevel));
        }
        return v;
    }

    @Override
    protected float act() {
        cam.update();


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
                //new CaffeineIndex(new DefaultConceptBuilder(rng), INDEX_SIZE, false, exe)
                new TreeIndex.L1TreeIndex(new DefaultConceptBuilder(rng), 8192, 3)

                , new FrameClock(), exe);

        nar.preprocess(new VariableCompressor.Precompressor(nar));

        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.8f);

        float p = 0.1f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.75f * p;
        nar.DEFAULT_GOAL_PRIORITY = 0.9f * p;
        nar.DEFAULT_QUESTION_PRIORITY = 0.1f * p;
        nar.DEFAULT_QUEST_PRIORITY = 0.1f * p;

        nar.cyclesPerFrame.set(cyclesPerFrame);
        nar.confMin.setValue(0.03f);
        nar.compoundVolumeMax.setValue(50);
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

        MySTMClustered stm = new MySTMClustered(nar, 128, '.', 3);
        MySTMClustered stmGoal = new MySTMClustered(nar, 128, '!', 3);

        //new ArithmeticInduction(nar);
        //new VariableCompressor(nar);



        Arkancide t = new Arkancide(nar);
        t.trace = true;


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