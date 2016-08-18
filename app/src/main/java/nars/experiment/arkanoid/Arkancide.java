package nars.experiment.arkanoid;


import nars.*;
import nars.data.AutoClassifier;
import nars.experiment.NAREnvironment;
import nars.gui.BeliefTableChart;
import nars.index.CaffeineIndex;
import nars.nar.Default;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.VariableCompressor;
import nars.op.time.MySTMClustered;
import nars.term.Compound;
import nars.term.obj.Termject;
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

import java.util.List;
import java.util.Random;

import static java.util.stream.Collectors.toList;
import static nars.$.t;
import static nars.experiment.tetris.Tetris2.arrayRenderer;
import static nars.experiment.tetris.Tetris2.exe;
import static nars.nal.UtilityFunctions.or;
import static nars.vision.PixelCamera.decodeRed;
import static spacegraph.obj.GridSurface.VERTICAL;

public class Arkancide extends NAREnvironment {

    private static final int cyclesPerFrame = 16;
    public static final int runFrames = 20000;
    public static final int CONCEPTS_FIRE_PER_CYCLE = 16;
    public static final int INDEX_SIZE = 3 * 100000;
    final Arkanoid noid;
    private final SwingCamera cam;

    private MotorConcept motorLeftRight;

    final int visW = 24;
    final int visH = 12;
    final SensorConcept[][] ss;

    private int visionSyncPeriod = 64;
    float noiseLevel = 0;

    float paddleSpeed = 45f;
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

        cam = new SwingCamera(noid, visW, visH);
        cam.update();



    }

    @Override
    protected void init(NAR n) {
        for (int x = 0; x < visW; x++) {
            int xx = x;
            for (int y = 0; y < visH; y++) {
                Compound squareTerm = $.p(new Termject.IntTerm(x), new Termject.IntTerm(y));
                int yy = y;
                SensorConcept sss;
                sensors.add(sss = new SensorConcept(squareTerm, nar,
                        () -> noise(decodeRed(cam.out.getRGB(xx, yy))) ,// > 0.5f ? 1 : 0,
                        (v) -> t(v, alpha)
                ));
                sss.sensor.dur = 0.1f;
                sss.timing(0,visionSyncPeriod);
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

            noid.paddle.move((motorLeftRight.goals().freq(now) - 0.5f) * paddleSpeed);
            return d;
            //return $.t((float)(noid.paddle.x / noid.SCREEN_WIDTH), 0.9f);

            //@Nullable Truth tNow = motorLeftRight.goals().truth(now);
            //if (tNow!=null)
                //noid.paddle.set(tNow.freq());

            //return $.t(noid.paddle.moveTo(d.freq(), paddleSpeed), 0.9f);
        }));

        //view.attention.add(nar.inputActivation);
        //view.attention.add(nar.derivedActivation);

        newBeliefChartWindow(this, 500);

        ControlSurface.newControlWindow(
                //new GridSurface(VERTICAL, actionTables),
                //BagChart.newBagChart((Default)nar, 1024),
                camView, view
        );

        //newConceptWindow((Default) n, 64, 4);

    }

    public static void newBeliefChartWindow(NAREnvironment narenv, long window) {
        GridSurface chart = newBeliefChart(narenv, window);
        new SpaceGraph().add(new Facial(chart).maximize()).show(800,600);
    }

    public static GridSurface newBeliefChart(NAREnvironment narenv, long window) {
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
                new CaffeineIndex(new DefaultConceptBuilder(rng), INDEX_SIZE, false, exe)
                , new FrameClock(), exe) {

            VariableCompressor.Precompressor p = new VariableCompressor.Precompressor(this);
            @Override protected Task preprocess(Task input) {
                return p.pre(input);
            }

        };
        nar.inputActivation.setValue(0.04f);
        nar.derivedActivation.setValue(0.04f);


        nar.beliefConfidence(0.8f);
        nar.goalConfidence(0.8f);
        nar.DEFAULT_BELIEF_PRIORITY = 0.15f;
        nar.DEFAULT_GOAL_PRIORITY = 0.6f;
        nar.DEFAULT_QUESTION_PRIORITY = 0.1f;
        nar.DEFAULT_QUEST_PRIORITY = 0.1f;
        nar.cyclesPerFrame.set(cyclesPerFrame);
        nar.confMin.setValue(0.03f);
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