package nars.experiment.tetris;

import nars.$;
import nars.NAR;
import nars.concept.ActionConcept;
import nars.concept.SensorConcept;
import nars.experiment.tetris.impl.TetrisState;
import nars.experiment.tetris.impl.TetrisVisualizer;
import nars.remote.NAgents;
import nars.term.Compound;
import nars.term.atom.Atomic;
import nars.time.FrameTime;
import nars.truth.Truth;
import nars.util.TaskStatistics;
import org.jetbrains.annotations.NotNull;
import spacegraph.math.v2;
import spacegraph.obj.widget.MatrixView;
import spacegraph.render.Draw;

import java.util.List;

import static nars.experiment.tetris.impl.TetrisState.*;
import static spacegraph.SpaceGraph.window;
import static spacegraph.obj.layout.Grid.grid;

/**
 * Created by me on 7/28/16.
 */
public class Tetris extends NAgents {

    //    static final Executioner exe =
//            //new SingleThreadExecutioner();
////            new MultiThreadExecutioner(2, 1024*8);
//            new MultiThreadExecutioner(3, 1024*8);


    public static final int tetris_width = 6;
    public static final int tetris_height = 20;
    public static final int TIME_PER_FALL = 16;
    public static final int PIXEL_RADIX = 2;
    private static SensorConcept[][] concept;
    private int afterlife = TIME_PER_FALL * tetris_height * tetris_width;
    static boolean easy = false;

    private final TetrisState state;

    //private final int visionSyncPeriod = 4; //16 * TIME_DILATION;

    public class View {

        public TetrisVisualizer vis;
        public MatrixView vis2 = new MatrixView(tetris_width, tetris_height, (x,y,gl)->{
            float r = nar.priority(concept[x][y]);
            gl.glColor3f(r, 0, 0);
            return 0f;
        });
        public MatrixView vis3 = new MatrixView(tetris_width, tetris_height, (x,y,gl)->{
            float r = concept[x][y].beliefFreq(now, 0.5f);
            gl.glColor3f(0, 0, r);
            return 0f;
        });
        public MatrixView vis4 = new MatrixView(tetris_width, tetris_height, (x,y,gl)->{
            long then = (long) (now + dur * 8);
            Draw.colorPolarized(gl,
                    concept[x][y].beliefFreq(then, 0.5f) -
                            concept[x][y].beliefFreq(now, 0.5f) );
            return 0f;
        });
//        public MatrixView vis4 = new MatrixView(tetris_width, tetris_height, (x,y,gl)->{
//            float r = concept[x][y].goalFreq(now, 0.5f);
//            gl.glColor3f(0, r, 0);
//            return 0f;
//        });

//        public Surface plot1;
//        //public ConsoleSurface term = new ConsoleSurface(40, 8);
//
//        public Surface plot2;
        //public Plot2D lstm;
    }

    final View view = new View();


    //private final ActionConcept motorRotate;
    //private MotorConcept motorDown;
    //private final ActionConcept motorLeftRight;

    //private final boolean rotate = !easy;

    /**
     * @param width
     * @param height
     * @param timePerFall larger is slower gravity
     */
    public Tetris(NAR nar, int width, int height, int timePerFall) {
        super("tetris", nar);

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

            @Override
            protected void die() {
                nar.time.tick(afterlife);
                super.die();
            }
        };
        view.vis = new TetrisVisualizer(state, 64, false) {
            @Override
            public boolean onKey(v2 hitPoint, char charCode, boolean pressed) {
//
//                switch (charCode) {
//                    case 'a':
//                        if (motorRotate!=null)
//                            nar.goal(motorRotate, Tense.Present, pressed ? 0f : 0.5f, gamma);
//                        break;
//                    case 's':
//                        if (motorRotate!=null)
//                            nar.goal(motorRotate, Tense.Present, pressed ? 1f : 0.5f, gamma);
//                        break;
//                    case 'z':
//                        nar.goal(motorLeftRight, Tense.Present, pressed ? 0f : 0.5f, gamma);
//                        break;
//                    case 'x':
//                        nar.goal(motorLeftRight, Tense.Present, pressed ? 1f : 0.5f, gamma);
//                        break;
//                }

                return true;
            }
        };


        sensors(nar, state, sensors);

        actions(nar, state, actions);

        state.reset();
    }



    public static void actions(NAR nar, TetrisState state, List<ActionConcept> actions) {
        float alpha = nar.confidenceDefault('.');

        float actionMargin =
                0.33f; //divides the range into 3 sections: left/nothing/right
        //0.25f;

        float actionThresholdHigh = 1f - actionMargin;
        float actionThresholdLow = actionMargin;
//        float actionThresholdHigher = 1f - actionMargin / 1.5f;
//        float actionThresholdLower = actionMargin / 1.5f;


        actions.add(new ActionConcept("moveX(tetris)", nar, (b, d) -> {
            if (d != null) {
                float x = d.freq();
                //System.out.println(d + " " + x);
                if (x > actionThresholdHigh) {
                    if (state.take_action(RIGHT))
                        //return d; //legal move
                        //return d.withConf(gamma);
                        return $.t(1, alpha);
                } else if (x < actionThresholdLow) {
                    if (state.take_action(LEFT))
                        //return d; //legal move
                        //return d.withConf(gamma);
                        return $.t(0, alpha);
                } else {
                    //return $.t(0.5f, alpha); //no action taken or move ineffective
                }
            }
            return $.t(0.5f, alpha); //no action taken or move ineffective
            //return null;
        }));

        //if (rotate) {
            actions.add(new ActionConcept("rotate(tetris)", nar, (b, d) -> {
                if (d != null) {
                    float r = d.freq();
                    if (r > actionThresholdHigh) {
                        if (state.take_action(CW))
                            //return d; //legal move
                            //return d.withConf(gamma);
                            return $.t(1, alpha);
                    } else if (r < actionThresholdLow) {
                        if (state.take_action(CCW))
                            //return d; //legal move
                            //return d.withConf(gamma);
                            return $.t(0, alpha);
                    } else {
                        //return $.t(0.5f, alpha); //no action taken or move ineffective
                    }
                }
                return $.t(0.5f, alpha); //no action taken or move ineffective
                //return null;
            }));
//        } else {
//            motorRotate = null;
//        }

        //actions.add(motorDown = new MotorConcept("(down)", nar));
//        if (downMotivation > actionThresholdHigh) {
//            state.take_action(FALL);
//        }
    }

    public static void sensors(NAR nar, TetrisState state, List<SensorConcept> sensors) {
        float alpha = nar.confidenceDefault('.');

        concept = new SensorConcept[state.width][state.height];

        Atomic tetris = $.the("tetris");
        for (int y = 0; y < state.height; y++) {
            int yy = y;
            for (int x = 0; x < state.width; x++) {
                int xx = x;
                Compound squareTerm =
                        //$.p(x, y);

                        $.inh(
                        //$.func(
                                $.p(
//                                  $.pRecurse($.radixArray(x, PIXEL_RADIX, state.width)), $.pRecurse($.radixArray(y, PIXEL_RADIX, state.height))
                                  //$.p($.radixArray(x, PIXEL_RADIX, state.width)), $.p($.radixArray(y, PIXEL_RADIX, state.height))
                                    x,y
                                ),
                                tetris
                        )
                                //$.p(
                                        //$.the("tetris"))
                                        //, $.the(state.time)))
                                ;

                //$.p($.pRadix(x, 4, state.width), $.pRadix(y, 4, state.height));
                int index = yy * state.width + xx;
                @NotNull SensorConcept s = new SensorConcept(squareTerm, nar,
                        () -> state.seen[index] > 0 ? 1f : 0f,

                        //null //disable input

                        (v) -> $.t(v, alpha)

                )
                        //timing(0, visionSyncPeriod)
                ;

//                FloatSupplier defaultPri = s.sensor.pri;
//                s.pri( () -> defaultPri.asFloat() * 0.25f );

                concept[x][y] = s;
                sensors.add(s);

            }
        }
    }

//    //TODO
//    public static class NARCam {
//        public int width;/*how wide our board is*/
//        public int height;/*how tall our board is*/
//
//
//    }

//    /**
//     * RLE/scanline input method: groups similar pixels (monochrome) into a runline using a integer range
//     */
//    protected void input() {
//
////        float thresh = 0.5f;
////
////        inputAxis(thresh, true);
////        inputAxis(thresh, false);
//    }

//    private void inputAxis(float thresh, boolean horizontal) {
//        int hh = horizontal ? state.height : state.width;
//        for (int y = 0; y < hh; ) {
//
//            int start = 0, end = 0;
//            int sign = 0;
//
//            int ww = horizontal ? state.width : state.height;
//            for (int x = 0; x < ww; ) {
//
//                int i;
//                if (horizontal)
//                    i = y * ww + x;
//                else
//                    i = x * hh + y;
//
//                float s = state.seen[i];
//
//                if (x == 0) {
//                    //beginning of span
//                    sign = (int) Math.signum(s);
//                } else {
//
//                    if (sign > 0) {
//                        if (s < (thresh)) {
//                            sign = -1;
//                        } else {
//                            end = x;  //continue span
//                        }
//                    }
//                    if (sign < 0) {
//                        if (s > (1f - thresh)) {
//                            sign = +1;
//                        } else {
//                            end = x; //continue span
//                        }
//                    }
//                }
//
//                //if it switched or reach the end of the line
//                if (end != x || (x >= ww - 1)) {
//                    //end of span
//                    if (end - start == 1) {
//                        inputBlock(start, start + 1, sign, horizontal);
//                    } else {
//                        inputSpan(start, end, y, sign, horizontal);
//                    }
//                }
//
//                x++;
//            }
//
//            y++;
//        }
//    }
//
//    private void inputSpan(int start, int end, int axis, int sign, boolean horizontal) {
//
//        Truth t = $.t(sign > 0 ? 1f : 0f,
//                //(float)Math.pow(alpha, end-start)
//                alpha
//        );
//        if (t == null)
//            return; //too low confidence
//
//        Term range = new Termject.IntInterval(start, end);
//        Term fixed = new IntTerm(axis);
//
//        //TODO collect evidence stamp
//        nar.believe(
//                horizontal ? $.p(range, fixed) : $.p(fixed, range),
//                Tense.Present,
//                t.freq(), t.conf() //HACK this parameters sux
//        );
//    }
//
//    private void inputBlock(int x, int y, float v, boolean horizontal) {
//
//        Truth t = $.t(v > 0 ? 1f : 0f,
//                //(float)Math.pow(alpha, end-start)
//                alpha
//        );
//        if (t == null)
//            return; //too low confidence
//
//        //TODO collect evidence stamp
//        nar.believe(
//                horizontal ? $.p(x, y) : $.p(y, x),
//                Tense.Present,
//                t.freq(), t.conf() //HACK this parameters sux
//        );
//    }


    @Override
    public float act() {

        state.next();

        return state.score;
    }



    public static void main(String[] args) {
        //Param.DEBUG = true;

        NAR nar = NAgents.newMultiThreadNAR(4, new FrameTime().dur(TIME_PER_FALL));
        nar.termVolumeMax.setValue(12);
        //nar.linkFeedbackRate.setValue(0.05f);

//        Random rng = new XorShift128PlusRandom(1);
//        //Multi nar = new Multi(3,512,
//        int maxVol = 32;
//
//        Executioner e = Tetris.exe;
//        ((MultiThreadExecutioner)exe).sync(false);
//
//        Default nar = new Default(1024,
//                256, 2, 3, rng,
//                //new CaffeineIndex(new DefaultConceptBuilder(rng), 1024*128, maxVol/2, false, e),
//                //new MapDBIndex(new DefaultConceptBuilder(rng), 200000, Executors.newSingleThreadScheduledExecutor()),
//                new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 200000, 8192, 2),
//                new FrameClock(), e
//        );
//
//
//        nar.beliefConfidence(0.9f);
//        nar.goalConfidence(0.9f);
//
//        Param.DEBUG_ANSWERS = Param.DEBUG;
//
////        nar.onTask(t -> {
////            long now = nar.time();
////            if (t.isBeliefOrGoal() && t.occurrence() > 1 + now) {
////                System.err.println("\tFUTURE: " + t + "\t vs. PRESENT: " +
////                        ((BeliefTable)(t.concept(nar).tableFor(t.punc()))).truth(now)
////                        //+ "\n" + t.proof() + "\n"
////                );
////            }
////        });
//
//        float p = 1f;
//        nar.DEFAULT_BELIEF_PRIORITY = 0.75f * p;
//        nar.DEFAULT_GOAL_PRIORITY = 1f * p;
//        nar.DEFAULT_QUESTION_PRIORITY = 0.5f * p;
//        nar.DEFAULT_QUEST_PRIORITY = 0.5f * p;
//
//
//        nar.confMin.setValue(0.02f);
//
//
//        nar.compoundVolumeMax.setValue(maxVol);
//        nar.linkFeedbackRate.setValue(0.15f);
//
//        //nar.truthResolution.setValue(0.02f);
//
////        nar.on(new TransformConcept("seq", (c) -> {
////            if (c.size() != 3)
////                return null;
////            Term X = c.term(0);
////            Term Y = c.term(1);
////
////            Integer x = intOrNull(X);
////            Integer y = intOrNull(Y);
////            Term Z = (x!=null && y!=null)? ((Math.abs(x-y) <= 1) ? $.the("TRUE") : $.the("FALSE")) : c.term(2);
////
////
////            return $.inh($.p(X, Y, Z), $.oper("seq"));
////        }));
////        nar.believe("seq(#1,#2,TRUE)");
////        nar.believe("seq(#1,#2,FALSE)");
//
//        //nar.log();
//        //nar.logSummaryGT(System.out, 0.1f);
//
////		nar.log(System.err, v -> {
////			if (v instanceof Task) {
////				Task t = (Task)v;
////				if (t instanceof DerivedTask && t.punc() == '!')
////					return true;
////			}
////			return false;
////		});
//
//
////        Abbreviation abbr = new Abbreviation(nar, "the", 3, 12,
////                0.01f, 64);
//
//        new Inperience(nar);
//
//        MySTMClustered stm = new MySTMClustered(nar, 64, '.', 4, true, 8);
//        MySTMClustered stmGoal = new MySTMClustered(nar, 16, '!', 2, true, 4);

        //new VariableCompressor(nar);


        Tetris t = new Tetris(nar, tetris_width, tetris_height, TIME_PER_FALL) {
            @Override
            protected void init() {
                super.init();
//                view.plot1 =
//                        Vis.emotionPlots(nar, 256);
//
//
//                view.plot2 = Vis.agentBudgetPlot(this, 256);
                window(grid(view.vis, view.vis2, view.vis3, view.vis4), 600, 600);
                NAgents.chart(this);
            }
        };

        t.trace = true;


        t.runRT(20f, 100).join();

//        NARController meta = new NARController(nar, loop, t);
//
//        newControlWindow(Lists.newArrayList(
//                newCPanel(nar, 256, () -> meta.rewardValue),
//                newBeliefChart(meta, 200)
//
//        ));



        //nar.stop();

        //nar.index.print(System.out);
        nar.tasks.forEach(System.out::println);

        //NAR.printActiveTasks(nar, true);
        //NAR.printActiveTasks(nar, false);
        nar.printConceptStatistics();
        new TaskStatistics().add(nar).print();

        //NAR.printTasks(meta.nar, true);
        //NAR.printTasks(meta.nar, false);
//        nar.forEachActiveConcept(c -> {
//            if (c.volume() < 12)
//                c.print();
//        });
    }

    public MatrixView.ViewFunction2D sensorMatrixView(NAR nar, long whenRelative) {
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

            float p = nar.priority(s);
            g.glColor4f(dr, dg, bf, 0.5f + 0.5f * p);

            return b != null ? b.conf() : 0;
        };
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
//
//        window(
//                col(
//
//                        //Vis.concepts(nar, 32),
//                        //Vis.agentActions(t, 1200),
//
//                        Vis.budgetHistogram(nar, 24)
//                        /*Vis.conceptLinePlot(nar,
//                                Iterables.concat(t.actions, Lists.newArrayList(t.happy, t.joy)),
//                                600)*/
//                ), 1200, 900);



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


//        nar.onFrame(f -> {
//            //view.lstm.update();
//            try {
//                view.term.term.putLinePre(t.summary());
//            } catch (IOException e1) {
//            }
//        });


//                int window = 32;
//                GridSurface camHistory = new GridSurface(HORIZONTAL,
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, -window * 2)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, -window)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, 0)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, +window)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, +window * 2))
//                );
//                newControlWindow(12f,4f, new Object[] { camHistory } );

//newControlWindow(view);


//newControlWindow(2f,4f, new Object[] { new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, 0)) } );

//Vis.newBeliefChartWindow(t, 200);

//window(Vis.budgetHistogram(nar, 30), 500, 300);

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


//t.run(runFrames);
//t.run(20000);
