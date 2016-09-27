package nars.experiment.tetris;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import nars.*;
import nars.concept.ActionConcept;
import nars.concept.SensorConcept;
import nars.experiment.tetris.visualizer.TetrisVisualizer;
import nars.gui.Vis;
import nars.index.CaffeineIndex;
import nars.nar.Default;
import nars.nar.exe.Executioner;
import nars.nar.exe.SingleThreadExecutioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.time.MySTMClustered;
import nars.remote.SwingAgent;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.obj.IntTerm;
import nars.term.obj.Termject;
import nars.time.FrameClock;
import nars.time.Tense;
import nars.truth.Truth;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.math.FloatSupplier;
import nars.video.CameraSensorView;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.NotNull;
import spacegraph.Surface;
import spacegraph.math.Vector2f;
import spacegraph.obj.ConsoleSurface;
import spacegraph.obj.GridSurface;
import spacegraph.obj.MatrixView;
import spacegraph.obj.Plot2D;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static nars.experiment.tetris.TetrisState.*;
import static spacegraph.SpaceGraph.window;
import static spacegraph.obj.ControlSurface.newControlWindow;
import static spacegraph.obj.GridSurface.VERTICAL;
import static spacegraph.obj.GridSurface.grid;

/**
 * Created by me on 7/28/16.
 */
public class Tetris extends NAgent {

    public static final int DEFAULT_INDEX_WEIGHT = 25 * 100000;

    public static final Executioner exe =
            new SingleThreadExecutioner();
//    public static final Executioner exe2 =
//            new MultiThreadExecutioner(2, 1024*8);
//    public static final Executioner exe4 =
//            new MultiThreadExecutioner(4, 1024*32);

    public static final int runFrames = 25550;
    public static final int cyclesPerFrame = 6;
    public static final int tetris_width = 8;
    public static final int tetris_height = 16;
    public static final int TIME_PER_FALL = 2;
    static boolean easy;

    static int frameDelay;


    private final TetrisState state;
    //private final int visionSyncPeriod = 4; //16 * TIME_DILATION;

    public static class View {

        public TetrisVisualizer vis;
        public Surface plot1;
        public ConsoleSurface term = new ConsoleSurface(40, 8);

        public Surface plot2;
        //public Plot2D lstm;
    }

    static final View view = new View();


    private final ActionConcept motorRotate;
    //private MotorConcept motorDown;
    private final ActionConcept motorLeftRight;
    private final boolean rotate = !easy;

    /**
     * @param width
     * @param height
     * @param timePerFall larger is slower gravity
     */
    public Tetris(NAR nar, int width, int height, int timePerFall) {
        super(nar, 8);

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
        view.vis = new TetrisVisualizer(state, 64, false) {
            @Override
            public boolean onKey(Vector2f hitPoint, char charCode, boolean pressed) {

                switch (charCode) {
                    case 'a':
                        if (motorRotate!=null)
                            nar.goal(motorRotate, Tense.Present, pressed ? 0f : 0.5f, gamma);
                        break;
                    case 's':
                        if (motorRotate!=null)
                            nar.goal(motorRotate, Tense.Present, pressed ? 1f : 0.5f, gamma);
                        break;
                    case 'z':
                        nar.goal(motorLeftRight, Tense.Present, pressed ? 0f : 0.5f, gamma);
                        break;
                    case 'x':
                        nar.goal(motorLeftRight, Tense.Present, pressed ? 1f : 0.5f, gamma);
                        break;
                }

                return true;
            }
        };

        state.seen = new float[state.width * state.height];

        for (int y = 0; y < state.height; y++) {
            int yy = y;
            for (int x = 0; x < state.width; x++) {
                int xx = x;
                Compound squareTerm =
                        //$.p(x, y);
                        $.inh($.p($.pRecurse($.radixArray(x, 2, state.width)), $.pRecurse($.radixArray(y, 2, state.height))),
                              $.the("tetris"));

                //$.p($.pRadix(x, 4, state.width), $.pRadix(y, 4, state.height));
                @NotNull SensorConcept s = new SensorConcept(squareTerm, nar,
                        () -> state.seen[yy * state.width + xx] > 0 ? 1f : 0f,

                        //null //disable input

                        (v) -> $.t(v, alpha)

                )
                        //timing(0, visionSyncPeriod)
                        ;

//                FloatSupplier defaultPri = s.sensor.pri;
//                s.pri( () -> defaultPri.asFloat() * 0.25f );

                sensors.add(s);

            }
        }


        float actionMargin =
                //0.33f; //divide the range into 3 sections: left/nothing/right
                0.25f;

        float actionThresholdHigh = 1f - actionMargin;
        float actionThresholdLow = actionMargin;
        float actionThresholdHigher = 1f - actionMargin / 1.5f;
        float actionThresholdLower = actionMargin / 1.5f;


        actions.add(motorLeftRight = new ActionConcept("(leftright)", nar, (b, d) -> {
            if (d != null) {
                float x = d.freq();
                //System.out.println(d + " " + x);
                if (x > actionThresholdHigh) {
                    if (state.take_action(RIGHT))
                        //return d; //legal move
                        //return d.withConf(gamma);
                        return $.t(1, gamma);
                } else if (x < actionThresholdLow) {
                    if (state.take_action(LEFT))
                        //return d; //legal move
                        //return d.withConf(gamma);
                        return $.t(0, gamma);
                }
            }
            //return null;
            return $.t(0.5f, gamma); //no action taken or move ineffective
        }));

        if (rotate) {
            actions.add(motorRotate = new ActionConcept("(rotate)", nar, (b, d) -> {
                if (d != null) {
                    float r = d.freq();
                    if (r > actionThresholdHigher) {
                        if (state.take_action(CW))
                            //return d; //legal move
                            //return d.withConf(gamma);
                            return $.t(1, gamma);
                    } else if (r < actionThresholdLower) {
                        if (state.take_action(CCW))
                            //return d; //legal move
                            //return d.withConf(gamma);
                            return $.t(0, gamma);
                    }
                }
                //return null;
                return $.t(0.5f, gamma); //no action taken or move ineffective
            }));
        } else {
            motorRotate = null;
        }
        //actions.add(motorDown = new MotorConcept("(down)", nar));
//        if (downMotivation > actionThresholdHigh) {
//            state.take_action(FALL);
//        }

        reset();
    }

//    //TODO
//    public static class NARCam {
//        public int width;/*how wide our board is*/
//        public int height;/*how tall our board is*/
//
//
//    }

    /**
     * RLE/scanline input method: groups similar pixels (monochrome) into a runline using a integer range
     */
    protected void input() {

//        float thresh = 0.5f;
//
//        inputAxis(thresh, true);
//        inputAxis(thresh, false);
    }

    private void inputAxis(float thresh, boolean horizontal) {
        int hh = horizontal ? state.height : state.width;
        for (int y = 0; y < hh; ) {

            int start = 0, end = 0;
            int sign = 0;

            int ww = horizontal ? state.width : state.height;
            for (int x = 0; x < ww; ) {

                int i;
                if (horizontal)
                    i = y * ww + x;
                else
                    i = x * hh + y;

                float s = state.seen[i];

                if (x == 0) {
                    //beginning of span
                    sign = (int) Math.signum(s);
                } else {

                    if (sign > 0) {
                        if (s < (thresh)) {
                            sign = -1;
                        } else {
                            end = x;  //continue span
                        }
                    }
                    if (sign < 0) {
                        if (s > (1f - thresh)) {
                            sign = +1;
                        } else {
                            end = x; //continue span
                        }
                    }
                }

                //if it switched or reach the end of the line
                if (end != x || (x >= ww - 1)) {
                    //end of span
                    if (end - start == 1) {
                        inputBlock(start, start + 1, sign, horizontal);
                    } else {
                        inputSpan(start, end, y, sign, horizontal);
                    }
                }

                x++;
            }

            y++;
        }
    }

    private void inputSpan(int start, int end, int axis, int sign, boolean horizontal) {

        Truth t = $.t(sign > 0 ? 1f : 0f,
                //(float)Math.pow(alpha, end-start)
                alpha
        );
        if (t == null)
            return; //too low confidence

        Term range = new Termject.IntInterval(start, end);
        Term fixed = new IntTerm(axis);

        //TODO collect evidence stamp
        nar.believe(
                horizontal ? $.p(range, fixed) : $.p(fixed, range),
                Tense.Present,
                t.freq(), t.conf() //HACK this parameters sux
        );
    }

    private void inputBlock(int x, int y, float v, boolean horizontal) {

        Truth t = $.t(v > 0 ? 1f : 0f,
                //(float)Math.pow(alpha, end-start)
                alpha
        );
        if (t == null)
            return; //too low confidence

        //TODO collect evidence stamp
        nar.believe(
                horizontal ? $.p(x, y) : $.p(y, x),
                Tense.Present,
                t.freq(), t.conf() //HACK this parameters sux
        );
    }


    @Override
    public float act() {


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

        input();

        return state.score;
    }

    public void reset() {
        state.reset();
        state.spawn_block();
        state.running = true;
    }


    public static void main(String[] args) {
        Param.DEBUG = false;

        Random rng = new XorShift128PlusRandom(1);
        //Multi nar = new Multi(3,512,
        int maxVol = 40;
        Executioner e = Tetris.exe;
        Default nar = new Default(1024,
                12, 2, 2, rng,
                new CaffeineIndex(new DefaultConceptBuilder(rng), 1024*128, maxVol/2, false, e),
                //new MapDBIndex(new DefaultConceptBuilder(rng), 200000, Executors.newSingleThreadScheduledExecutor()),
                //new TreeIndex.L1TreeIndex(new DefaultConceptBuilder(rng), 200000, 8192, 2),
                new FrameClock(), e
        );


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.8f);

        Param.DEBUG_ANSWERS = Param.DEBUG;

//        nar.onTask(t -> {
//            long now = nar.time();
//            if (t.isBeliefOrGoal() && t.occurrence() > 1 + now) {
//                System.err.println("\tFUTURE: " + t + "\t vs. PRESENT: " +
//                        ((BeliefTable)(t.concept(nar).tableFor(t.punc()))).truth(now)
//                        //+ "\n" + t.proof() + "\n"
//                );
//            }
//        });

        float p = 0.1f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.8f * p;
        nar.DEFAULT_GOAL_PRIORITY = 1f * p;
        nar.DEFAULT_QUESTION_PRIORITY = 0.25f * p;
        nar.DEFAULT_QUEST_PRIORITY = 0.3f * p;
        nar.cyclesPerFrame.set(cyclesPerFrame);

        nar.confMin.setValue(0.02f);

        //Abbreviation abbr = new Abbreviation(nar, "the", 4, 0.5f, 32);

        nar.compoundVolumeMax.setValue(maxVol);
        //nar.linkFeedbackRate.setValue(0.95f);

        //nar.truthResolution.setValue(0.02f);

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


        //new Abbreviation(nar,"aKa_");
        //new Abbreviation2(nar, "_");

        MySTMClustered stm = new MySTMClustered(nar, 128, '.', 3);
        MySTMClustered stmGoal = new MySTMClustered(nar, 64, '!', 2);

        //new VariableCompressor(nar);


        Tetris t = new Tetris(nar, tetris_width, tetris_height, TIME_PER_FALL);


//                AutoClassifier ac = new AutoClassifier($.the("row"), nar, sensors,
//                        tetris_width/2, 7 /* states */,
//                        0.05f);
//                view.autoenc = new MatrixView(ac.W.length, ac.W[0].length, arrayRenderer(ac.W));

//                int totalSize = tetris_width*tetris_height;
//                AutoClassifier bc = new AutoClassifier($.the("row4"), nar, sensors,
//                        tetris_width*4, 16,
//                        0.1f);

//                newControlWindow(
//                        new GridSurface(VERTICAL,
//                                charted.stream().map(c -> new BeliefTableChart(nar, c)).collect(toList())
//                        )
//                );


        //window(Vis.concepts(nar, 1024), 500, 500);

        //STMView.show(stm, 800, 600);

        window(
                grid(

                        //Vis.concepts(nar, 32),
                        Vis.agentActions(t, 250),

                        Vis.budgetHistogram(nar, 10),
                        conceptLinePlot(nar,
                                Iterables.concat(t.actions, Lists.newArrayList(t.happy, t.joy)),
                                200)
                ), 1200, 900);


        view.plot1 =
                newCPanel(nar, 256, () -> t.rewardValue);


        view.plot2 = agentBudgetPlot(t, 256);
            /*view.plot2 = new GridSurface(HORIZONTAL,
                //conceptLinePlot(nar, Lists.newArrayList( t.happy, t.joy ), (c) -> nar.conceptPriority(c), 256),

                conceptLinePlot(nar, t.actions, (c) -> {
                    try {
                        return nar.concept(c).goals().truth(nar.time()).freq();
                    } catch (NullPointerException npe) {
                        return 0.5f;
                    }
                }, 256)
            );*/

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
            //view.lstm.update();
            try {
                view.term.term.putLinePre(t.summary());
            } catch (IOException e1) {
            }
        });


//                int window = 32;
//                GridSurface camHistory = new GridSurface(HORIZONTAL,
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, -window * 2)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, -window)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, 0)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, +window)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, +window * 2))
//                );
//                newControlWindow(12f,4f, new Object[] { camHistory } );

        newControlWindow(view);

        //newControlWindow(2f,4f, new Object[] { new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, 0)) } );

        //Vis.newBeliefChartWindow(t, 200);

        window(Vis.budgetHistogram(nar, 30), 500, 300);

        //Arkancide.newBeliefChartWindow(nar, 200, nar.inputTask("(&&, ((happy) ==>+0 (joy)), ((joy) ==>+0 (happy)), ((happy) <=>+0 (joy))). :|:").term());

//                BeliefTableChart.newBeliefChart(nar, Lists.newArrayList(
//                        sensors.get(0),
//                        sensors.get(1),
//                        sensors.get(2),
//                        sensors.get(3),
//                        sensors.get(4),
//                        sensors.get(5)
//                ), 200);

        //NARSpace.newConceptWindow((Default) nar, 32, 8);


        t.trace = true;

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


        NARLoop loop = t.run(runFrames, frameDelay);

//        NARController meta = new NARController(nar, loop, t);
//
//        newControlWindow(Lists.newArrayList(
//                newCPanel(nar, 256, () -> meta.rewardValue),
//                newBeliefChart(meta, 200)
//
//        ));

        loop.join();


        //nar.stop();

        //nar.index.print(System.out);
        NAR.printTasks(nar, true);
        NAR.printTasks(nar, false);
        nar.printConceptStatistics();

        //NAR.printTasks(meta.nar, true);
        //NAR.printTasks(meta.nar, false);
//        nar.forEachActiveConcept(c -> {
//            if (c.volume() < 12)
//                c.print();
//        });
    }

    public static GridSurface agentBudgetPlot(NAgent t, int history) {
        return conceptLinePlot(t.nar,
                Iterables.concat(t.actions, Lists.newArrayList(t.happy, t.joy)), history);
    }

    public MatrixView.ViewFunc sensorMatrixView(NAR nar, long whenRelative) {
        return (x, y, g) -> {
//            int rgb = cam.out.getRGB(x,y);
//            float r = decodeRed(rgb);
//            if (r > 0)
//                System.out.println(x + " "+ y + " " + r);
//            g.glColor3f(r,0,0);

            SensorConcept s = sensors.get(y * tetris_width + x);

            Truth b = s.beliefs().truth(now + whenRelative);
            float bf = b != null ? b.freq() : 0.5f;
            Truth dt = s.goals().truth(now + whenRelative);
            float dr, dg;
            if (dt == null) {
                dr = dg = 0;
            } else {
                float f = dt.freq();
                float c = dt.conf();
                if (f > 0.5f) {
                    dr = 0;
                    dg = (f - 0.5f) * 2f;// * c;
                } else {
                    dg = 0;
                    dr = (0.5f - f) * 2f;// * c;
                }
            }

            float p = nar.conceptPriority(s);
            g.glColor4f(dr, dg, bf, 0.5f + 0.5f * p);

            return b != null ? b.conf() : 0;
        };
    }



    public static GridSurface newCPanel(NAR nar, int plotHistory, FloatSupplier reward) {
        Plot2D plot = new Plot2D(plotHistory, Plot2D.Line);
        plot.add("Rwrd", reward);

        Plot2D plot1 = new Plot2D(plotHistory, Plot2D.Line);
        plot1.add("Conf", () -> nar.emotion.confident.getSum());

        Plot2D plot2 = new Plot2D(plotHistory, Plot2D.Line);
        plot2.add("Busy", () -> nar.emotion.busy.getSum());
        plot2.add("Lern", () -> nar.emotion.busy.getSum() - nar.emotion.frustration.getSum());

        Plot2D plot3 = new Plot2D(plotHistory, Plot2D.Line);
        plot3.add("Strs", () -> nar.emotion.stress.getSum());


        Plot2D plot4 = new Plot2D(plotHistory, Plot2D.Line);
        plot4.add("Hapy", () -> nar.emotion.happy.getSum());
        plot4.add("Sad", () -> nar.emotion.sad.getSum());

//                Plot2D plot4 = new Plot2D(plotHistory, Plot2D.Line);
//                plot4.add("Errr", ()->nar.emotion.errr.getSum());

        nar.onFrame(f -> {
            plot.update();
            plot1.update();
            plot2.update();
            plot3.update();
            plot4.update();
        });

        return new GridSurface(VERTICAL, plot, plot1, plot2, plot3, plot4);
    }

    public static GridSurface conceptLinePlot(NAR nar, Iterable<? extends Termed> concepts, int plotHistory, FloatFunction<Termed> value) {

        //TODO make a lambda Grid constructor
        GridSurface grid = new GridSurface();
        List<Plot2D> plots = $.newArrayList();
        for (Termed t : concepts) {
            Plot2D p = new Plot2D(plotHistory, Plot2D.Line /*BarWave*/);
            p.add(t.toString(), ()->value.floatValueOf(t), 0f, 1f );
            grid.children.add(p);
            plots.add(p);
        }
        grid.layout();

        nar.onFrame(f -> {
            plots.forEach(Plot2D::update);
        });

        return grid;
    }
    public static GridSurface conceptLinePlot(NAR nar, Iterable<? extends Termed> concepts, int plotHistory) {

        //TODO make a lambda Grid constructor
        GridSurface grid = new GridSurface();
        List<Plot2D> plots = $.newArrayList();
        for (Termed t : concepts) {
            Plot2D p = new Plot2D(plotHistory, Plot2D.Line /*BarWave*/);
            p.setTitle(t.toString());
            p.add("P", ()->nar.conceptPriority(t), 0f, 1f );
            p.add("B", ()->nar.concept(t).beliefFreq(nar.time()), 0f, 1f );
            p.add("G", ()->nar.concept(t).goalFreq(nar.time()), 0f, 1f );
            grid.children.add(p);
            plots.add(p);
        }
        grid.layout();

        nar.onFrame(f -> {
            plots.forEach(Plot2D::update);
        });

        return grid;
    }

//    public static class NARController extends NAgent {
//
//        private final NARLoop loop;
//        private final NAR worker;
//        private final NAgent env;
//        private final FloatSupplier learn;
//        private final RangeNormalizedFloat busy;
//        public float score;
//
//
//        @Override
//        protected float act() {
//            //float avgFramePeriodMS = (float) loop.frameTime.getMean();
//
//            //float mUsage = memory();
//            //float targetMemUsage = 0.75f;
//
//            return this.score = (
//                    (1f + learn.asFloat()) *       //learn
////                    (1f + (1f- busy.asFloat())) *  //avoid busywork
//                    (1f + happysad.asFloat())      //boost for motivation change
//
//                    //env.rewardNormalized.asFloat() +
//
////                    1 / (1f + Math.abs(targetMemUsage - mUsage) / (targetMemUsage)) //maintain % memory utilization TODO cache 'memory()' result
//            );
//        }
//
//
//        public NARController( NAR worker, NARLoop loop, NAgent env) {
//
//            super( new Default(384, 4, 3, 2, new XORShiftRandom(2),
//                    //new CaffeineIndex(new DefaultConceptBuilder(new XORShiftRandom(3)), 5*100000, false, exe),
//                    new TreeIndex.L1TreeIndex(new DefaultConceptBuilder(new XORShiftRandom(3)), 100000, 16384, 4),
//
//                    new FrameClock()) {
//                       @Override
//                       protected void initHigherNAL() {
//                           super.initHigherNAL();
//                           cyclesPerFrame.setValue(16);
//                           //ctl.confMin.setValue(0.01f);
//                           //ctl.truthResolution.setValue(0.01f);
//                           beliefConfidence(0.5f);
//                           goalConfidence(0.5f);
//                       }
//                   });
//
//            this.worker = worker;
//            this.loop = loop;
//            this.env = env;
//
//            busy = new RangeNormalizedFloat(()->(float)worker.emotion.busy.getSum());
//            happysad = new RangeNormalizedFloat(()->(float)worker.emotion.happysad());
//            learn = ()->(float)worker.emotion.learning();
//
//            //nar.log();
//            worker.onFrame(nn -> next());
//
//            init(nar);
//            mission();
//        }
//
//        @Override public void next() {
//            super.next();
//            nar.next();
//        }
//
//
//
//        @Override
//        protected void init(NAR n) {
//
//            float sensorResolution = 0.05f;
//
//            float sensorConf = alpha;
//
//            FloatToObjectFunction<Truth> truther = (v) -> $.t(v, sensorConf);
//
//            sensors.addAll(Lists.newArrayList(
//                    new SensorConcept("(motive)", n,
//                            happysad,
//                            truther
//                    ).resolution(sensorResolution),
//                    new SensorConcept("(busy)", n,
//                            new RangeNormalizedFloat(() -> (float) worker.emotion.busy.getSum()),
//                            truther
//                    ).resolution(sensorResolution),
//                    new SensorConcept("(learn)", n,
//                            learn,
//                            truther
//                    ).resolution(sensorResolution),
//                    new SensorConcept("(memory)", n,
//                            () -> memory(),
//                            truther
//                    ).resolution(sensorResolution)
//            ));
//
//            //final int BASE_PERIOD_MS = 100;
//            final int MAX_CONCEPTS_FIRE_PER_CYCLE = 32;
//            final int MAX_LINKS_PER_CONCEPT = 24;
//
//            actions.addAll(Lists.newArrayList(
//                    //cpu throttle
//                    /*new MotorConcept("(cpu)", nar, (b, d) -> {
//                        int newPeriod = Math.round(((1f - (d.expectation())) * BASE_PERIOD_MS));
//                        loop.setPeriodMS(newPeriod);
//                        //System.err.println("  loop period ms: " + newPeriod);
//                        return d;
//                    }),*/
//
//                    //memory throttle
////                    new MotorConcept("(memoryWeight)", nar, (b, d) -> {
////                        ((CaffeineIndex) worker.index).compounds.policy().eviction().ifPresent(e -> {
////                            float sweep = 0.1f; //% sweep , 0<sweep
////                            e.setMaximum((long) (DEFAULT_INDEX_WEIGHT * (1f + sweep * 2f * (d.freq() - 0.5f))));
////                        });
////                        //System.err.println("  loop period ms: " + newPeriod);
////                        return d;
////                    }),
//
//                    new MotorConcept("(confMin)", nar, (b, d) -> {
//                        float MAX_CONFMIN = 0.1f;
//                        float newConfMin = Math.max(Param.TRUTH_EPSILON, MAX_CONFMIN * d.freq());
//                        worker.confMin.setValue(newConfMin);
//                        return d;
//                    }),
////
////                    new MotorConcept("(inputActivation)", nar, (b, d) -> {
////                        worker.inputActivation.setValue(d.freq());
////                        return d;
////                    }),
////
////                    new MotorConcept("(derivedActivation)", nar, (b, d) -> {
////                        worker.derivedActivation.setValue(d.freq());
////                        return d;
////                    }),
//
//                    new MotorConcept("(conceptsPerFrame)", nar, (b, d) -> {
//                        ((Default) worker).core.conceptsFiredPerCycle.setValue((int) (d.freq() * MAX_CONCEPTS_FIRE_PER_CYCLE));
//                        return d;
//                    }),
//
//                    new MotorConcept("(linksPerConcept)", nar, (b, d) -> {
//                        float l = d.freq() * MAX_LINKS_PER_CONCEPT;
//                        l = Math.max(l, 1f);
//                        int vv = (int) Math.floor((float)Math.sqrt(l));
//
//                        ((Default) worker).core.tasklinksFiredPerFiredConcept.setValue(vv);
//                        ((Default) worker).core.termlinksFiredPerFiredConcept.setValue((int)Math.ceil(l / vv));
//                        return d;
//                    }),
//
//                    new MotorConcept("(envCuriosity)", nar, (b, d) -> {
//                        float exp = d.freq();
//                        env.epsilonProbability = exp;
//                        env.gammaEpsilonFactor = exp*exp;
//                        return d;
//                    })
//            ));
//        }
//
//        public final float memory() {
//            Runtime runtime = Runtime.getRuntime();
//            long total = runtime.totalMemory(); // current heap allocated to the VM process
//            long free = runtime.freeMemory(); // out of the current heap, how much is free
//            long max = runtime.maxMemory(); // Max heap VM can use e.g. Xmx setting
//            long usedMemory = total - free; // how much of the current heap the VM is using
//            long availableMemory = max - usedMemory; // available memory i.e. Maximum heap size minus the current amount used
//            float ratio = 1f - ((float)availableMemory) / max;
//            //logger.warn("max={}k, used={}k {}%, free={}k", max/1024, total/1024, Texts.n2(100f * ratio), free/1024);
//            return ratio;
//        }
//
//        final RangeNormalizedFloat happysad;
//
//    }
//
//
//    //    static void addCamera(Tetris t, NAR n, int w, int h) {
////        //n.framesBeforeDecision = GAME_DIVISOR;
////        SwingCamera s = new SwingCamera(t.vis);
////
////        NARCamera nc = new NARCamera("t", n, s, (x, y) -> $.p($.the(x), $.the(y)));
////
////        NARCamera.newWindow(s);
////
////        s.input(0, 0, t.vis.getWidth(),t.vis.getHeight());
////        s.output(w, h);
////
////        n.onFrame(nn -> {
////            s.update();
////        });
////    }

}
