package nars.experiment.tetris;

import nars.$;
import nars.NAR;
import nars.Param;
import nars.experiment.NAREnvironment;
import nars.experiment.tetris.visualizer.TetrisVisualizer;
import nars.index.CaffeineIndex;
import nars.nar.Default;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.VariableCompressor;
import nars.op.time.MySTMClustered;
import nars.task.Task;
import nars.term.Compound;
import nars.term.obj.Termject;
import nars.time.FrameClock;
import nars.truth.Truth;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import spacegraph.Surface;
import spacegraph.obj.ConsoleSurface;
import spacegraph.obj.GridSurface;
import spacegraph.obj.MatrixView;
import spacegraph.obj.Plot2D;

import java.io.IOException;
import java.util.*;

import static nars.$.t;
import static nars.experiment.arkanoid.Arkancide.newBeliefChart;
import static nars.experiment.tetris.TetrisState.*;
import static spacegraph.obj.ControlSurface.newControlWindow;
import static spacegraph.obj.GridSurface.VERTICAL;

/**
 * Created by me on 7/28/16.
 */
public class Tetris2 extends NAREnvironment {

    public static final int TIME_DILATION = 1; //resolution in between frames for interpolation space

    static {
        Param.DEBUG = true;
        Param.CONCURRENCY_DEFAULT = 4;
    }

    public static final int runFrames = 1000;
    public static final int cyclesPerFrame = 16;
    public static final int tetris_width = 6;
    public static final int tetris_height = 12;
    public static final int TIME_PER_FALL = 3;
    static int frameDelay = 0;

    static boolean easy = true;


    private final TetrisState state;
    private int visionSyncPeriod = 8 * TIME_DILATION;

    public class View {

        public TetrisVisualizer vis;
        public Surface plot1;
        public ConsoleSurface term = new ConsoleSurface(40, 8);

        public MatrixView camView;
        //public Plot2D lstm;
    }

    final View view = new View();


    private MotorConcept motorRotate;
    //private MotorConcept motorDown;
    private MotorConcept motorLeftRight;
    private final boolean rotate = !easy;

    /**
     * @param width
     * @param height
     * @param timePerFall larger is slower gravity
     */
    public Tetris2(NAR nar, int width, int height, int timePerFall) {
        super(nar);

        state = new TetrisState(width, height, timePerFall) {
            @Override
            protected int nextBlock() {


                if (easy) {
                    //EASY MODE
                    return 1; //square blocks
                    //return 0; //long blocks
                } else {
                    return super.nextBlock(); //all blocks
                }
            }
        };
        view.vis = new TetrisVisualizer(state, 64, false);

        //restart();
    }


    @Override
    protected void init(NAR nar) {

        state.seen = new float[state.width * state.height];
        for (int y = 0; y < state.height; y++) {
            int yy = y;
            for (int x = 0; x < state.width; x++) {
                int xx = x;
                Compound squareTerm = $.p(new Termject.IntTerm(x), new Termject.IntTerm(y));
                sensors.add(new SensorConcept(squareTerm, nar,
                        () -> state.seen[yy * state.width + xx] > 0 ? 1f : 0f,
                        (v) -> t(v, alpha)
                ).timing(0, visionSyncPeriod));

            }
        }


        if (rotate) {
            actions.add(motorRotate = new MotorConcept("(rotate)", nar));
        }
        //actions.add(motorDown = new MotorConcept("(down)", nar));
        actions.add(motorLeftRight = new MotorConcept("(leftright)", nar));

        reset();
    }

    @Override
    public float act() {


        float rotateMotivation = (motorRotate != null && motorRotate.hasGoals()) ? motorRotate.goals().expectation(now) : 0.5f;
        //float downMotivation = motorDown.hasGoals() ? motorDown.goals().expectation(now) : 0.5f;
        float leftRightMotivation = motorLeftRight.hasGoals() ? motorLeftRight.goals().expectation(now) : 0.5f;

        float actionMargin = 0.35f;
        float actionThresholdHigh = 1f - actionMargin;
        float actionThresholdLow = actionMargin;

        if (rotateMotivation > actionThresholdHigh) {
            state.take_action(CW);
        } else if (rotateMotivation < actionThresholdLow) {
            state.take_action(CCW);
        }
//        if (downMotivation > actionThresholdHigh) {
//            state.take_action(FALL);
//        }

        if (leftRightMotivation > actionThresholdHigh) {
            state.take_action(RIGHT);
        } else if (leftRightMotivation < actionThresholdLow) {
            state.take_action(LEFT);
        }

        if (state.running) {
            state.take_action(-1); //actions already taken above
            state.update();
        } else {
            state.spawn_block();
        }

        state.checkIfRowAndScore();

        state.toVector(false, state.seen);


        if (state.gameOver()) {
            reset();
        }

        return state.score;
    }

    public void reset() {
        state.reset();
        state.spawn_block();
        state.running = true;
    }


    public static void main(String[] args) {
        Random rng = new XorShift128PlusRandom(1);

        //Multi nar = new Multi(3,512,
        Default nar = new Default(2048,
                32, 2, 2, rng,
                new CaffeineIndex(new DefaultConceptBuilder(rng), 5 * 1000000, false)

                , new FrameClock()) {

            VariableCompressor.Precompressor p = new VariableCompressor.Precompressor(this);

            @Override
            protected Task preprocess(Task input) {
                return p.pre(input);
            }

        };

        nar.inputActivation.setValue(0.05f);
        nar.derivedActivation.setValue(0.05f);


        nar.beliefConfidence(0.7f);
        nar.goalConfidence(0.7f);
        nar.DEFAULT_BELIEF_PRIORITY = 0.15f;
        nar.DEFAULT_GOAL_PRIORITY = 0.5f;
        nar.DEFAULT_QUESTION_PRIORITY = 0.3f;
        nar.DEFAULT_QUEST_PRIORITY = 0.4f;
        nar.cyclesPerFrame.set(cyclesPerFrame);
        nar.confMin.setValue(0.05f);
        nar.truthResolution.setValue(0.05f);

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
        MySTMClustered stmGoal = new MySTMClustered(nar, 64, '!', 2);

        //new ArithmeticInduction(nar);
        //new VariableCompressor(nar);



        Tetris2 t = new Tetris2(nar, tetris_width, tetris_height, TIME_PER_FALL) {

            @Override
            public void init(NAR nar) {
                super.init(nar);


//                newControlWindow(
//                        new GridSurface(VERTICAL,
//                                charted.stream().map(c -> new BeliefTableChart(nar, c)).collect(toList())
//                        )
//                );


                //BagChart.show((Default) nar, 1024);
                //STMView.show(stm, 800, 600);

                int plotHistory = 256;
                Plot2D plot = new Plot2D(plotHistory, Plot2D.Line);
                plot.add("Rwrd", () -> rewardValue);

                Plot2D plot2 = new Plot2D(plotHistory, Plot2D.Line);
                plot2.add("Busy", () -> nar.emotion.busy.getSum());
                plot2.add("Lern", () -> nar.emotion.busy.getSum() - nar.emotion.frustration.getSum());
                //plot2.add("Strs", ()->nar.emotion.stress.getSum());

                Plot2D plot3 = new Plot2D(plotHistory, Plot2D.Line);
                plot3.add("Hapy", () -> nar.emotion.happy.getSum());

//                Plot2D plot4 = new Plot2D(plotHistory, Plot2D.Line);
//                plot4.add("Errr", ()->nar.emotion.errr.getSum());


                view.plot1 = new GridSurface(VERTICAL, plot, plot2, plot3);



//                {
//                    List<FloatSupplier> li = new ArrayList();
//                    for (int i = 0; i < sensors.size(); i++) {
//                        li.add(sensors.get(i).getInput());
//                    }
//
//                    List<FloatSupplier> lo = new ArrayList();
//                    RangeNormalizedFloat normReward = new RangeNormalizedFloat(() -> rewardValue);
//                    lo.add(normReward);
////
////
//                    LSTMPredictor lp = new LSTMPredictor(li, lo, 1);
////
//
//                    double[] lpp = new double[2];
//                    nar.onFrame(nn -> {
//                        double[] p = lp.next();
//                        System.arraycopy(p, 0, lpp, 0, p.length);
//                        //System.out.println("LSTM: " + Texts.n4(p) + " , " + normReward.asFloat());
//                    });
//
//                    view.lstm = new Plot2D(plotHistory, Plot2D.Line)
//                            .add("Reward (actual)", () -> normReward.asFloat())
//                            .add("Predicted", () -> lpp[0]);
//
//
//                }

                nar.onFrame(f -> {
                    plot.update();
                    plot2.update();
                    plot3.update();
                    //view.lstm.update();
                    try {
                        view.term.term.putLinePre(summary());
                    } catch (IOException e) {
                    }
                });


                view.camView = new MatrixView(tetris_width, tetris_height, (x, y, g) -> {
//            int rgb = cam.out.getRGB(x,y);
//            float r = decodeRed(rgb);
//            if (r > 0)
//                System.out.println(x + " "+ y + " " + r);
//            g.glColor3f(r,0,0);

                    SensorConcept s = sensors.get(y * tetris_width + x);
                    float b = s.hasBeliefs() ? s.beliefs().expectation(now) : 0;
                    Truth dt = s.hasGoals() ? s.goals().truth(now) : null;
                    float dr, dg;
                    if (dt == null) {
                        dr = dg = 0;
                    } else {
                        float f = dt.freq();
                        float c = dt.conf();
                        if (f > 0.5f) {
                            dr = 0;
                            dg = (f - 0.5f) * 2f * c;
                        } else {
                            dg = 0;
                            dr = (0.5f - f) * 2f * c;
                        }
                    }

                    float p = nar.conceptPriority(s);
                    g.glColor4f(dr, dg, b, 0.5f + 0.5f * p);

                });

                newControlWindow(view);

                newBeliefChart(this, 11500);

                //NARSpace.newConceptWindow((Default) nar, 128, 8);
            }
        };


//        Iterable<Termed> cheats = Iterables.concat(
//                numericSensor(() -> t.currentX, nar, 0.3f,
//                        "(cursor_x)")
//                        //"(active,a)","(active,b)","(active,c)","(active,d)","(active,e)","(active,f)","(active,g)","(active,h)")
//                        //"I(a)","I(b)","I(c)","I(d)","I(e)","I(f)","I(g)","I(h)")
//                        //"(active,x)")
//                        .resolution(0.5f / t.width),
//                numericSensor(() -> t.currentY, nar, 0.3f,
//                        "(cursor_y)")
//                        //"active:(y,t)", "active:(y,b)")
//                        //"(active,y)")
//                        .resolution(0.5f / t.height)
//        );

//        NAgent n = new NAgent(nar) {
//            @Override
//            public void start(int inputs, int actions) {
//                super.start(inputs, actions);
//
//                List<Termed> charted = new ArrayList(super.actions);
//
//                charted.add(sad);
//                charted.add(happy);
//                Iterables.addAll(charted, cheats);
//
//                if (nar instanceof Default) {
//
//                    new BeliefTableChart(nar, charted).show(600, 900);
//
//                    //BagChart.show((Default) nar, 128);
//
//                    //STMView.show(stm, 800, 600);
//
//
//                    NARSpace.newConceptWindow((Default) nar, 128, 8);
//                }
//
//
//            }
//
//            @Override
//            protected Collection<Task> perceive(Set<Task> inputs) {
//                return super.perceive(inputs);
//            }
//        };


        //addCamera(t, nar, 8, 8);


        t.run(runFrames, frameDelay, TIME_DILATION).join();
        //nar.stop();

        //nar.index.print(System.out);
        NAR.printTasks(nar, true);
        NAR.printTasks(nar, false);
        //nar.forEachActiveConcept(System.out::println);
    }

    //    static void addCamera(Tetris t, NAR n, int w, int h) {
//        //n.framesBeforeDecision = GAME_DIVISOR;
//        SwingCamera s = new SwingCamera(t.vis);
//
//        NARCamera nc = new NARCamera("t", n, s, (x, y) -> $.p($.the(x), $.the(y)));
//
//        NARCamera.newWindow(s);
//
//        s.input(0, 0, t.vis.getWidth(),t.vis.getHeight());
//        s.output(w, h);
//
//        n.onFrame(nn -> {
//            s.update();
//        });
//    }

}
