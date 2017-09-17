package nars;

import jcog.Util;
import jcog.data.FloatParam;
import jcog.event.On;
import jcog.pri.mix.control.MixContRL;
import nars.control.Cause;
import nars.control.Derivation;
import nars.control.NARService;
import nars.derive.Deriver;
import nars.derive.PrediTerm;
import nars.exe.FocusExec;
import nars.exe.MultiExec;
import nars.gui.Vis;
import nars.index.term.map.CaffeineIndex;
import nars.op.data.reflect;
import nars.op.mental.Abbreviation;
import nars.op.mental.Inperience;
import nars.op.stm.MySTMClustered;
import nars.op.stm.STMLinkage;
import nars.term.Term;
import nars.time.RealTime;
import nars.truth.Truth;
import nars.video.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;
import spacegraph.layout.Grid;
import spacegraph.render.Draw;
import spacegraph.widget.console.ConsoleTerminal;
import spacegraph.widget.meta.WindowButton;
import spacegraph.widget.meter.BitmapMatrixView;
import spacegraph.widget.meter.Plot2D;
import spacegraph.widget.meter.TreeChart;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import static nars.$.$;
import static nars.Op.BELIEF;
import static spacegraph.SpaceGraph.window;
import static spacegraph.layout.Grid.col;
import static spacegraph.layout.Grid.grid;

/**
 * Extensions to NAgent interface:
 * <p>
 * --chart output (spacegraph)
 * --cameras (Swing and OpenGL)
 */
abstract public class NAgentX extends NAgent {


    public final Set<CameraSensor> cam = new LinkedHashSet<>();

    public NAgentX(NAR nar) {
        this("", nar);
    }

    public NAgentX(String id, NAR nar) {
        super(id, nar);
    }

    public NAgentX(Term id, NAR nar) {
        super(id, nar);
    }

//    public static void run(Function<NAR, NAgentX> init, int frames) {
//        Default nar = NARBuilder.newMultiThreadNAR(3, new FrameTime(), true);
//        //Default nar = newNAR();
//        //Default2 nar = newNAR2();
//
//        NAgentX a = init.apply(nar);
//        a.trace = true;
//
//
//        chart(a);
//
//        a.run(frames);
//
//        print(nar, a);
//
//
//        //((TreeTaskIndex)nar.tasks).tasks.prettyPrint(System.out);
//
//    }

//    private static void print(NAR nar, NAgentX a) {
//        //NAR.printActiveTasks(nar, true);
//        //NAR.printActiveTasks(nar, false);
//
//        nar.forEachTask(x -> {
//            System.out.println(x);
//            //if (x.isQuestOrQuestion())
//            ///System.out.println(x.proof());
//        });
//
//        nar.printConceptStatistics();
//        new TaskStatistics().add(nar).print(System.out);
//
//    }


    public static NAR runRT(Function<NAR, NAgent> init, float fps) {
        return runRT(init, fps, -1);
    }


    public static NAR runRT(Function<NAR, NAgent> init, float fps, long endTime) {

        //Builder.Subterms.the = Builder.Subterms.WeakSubtermBuilder.get();
        //Builder.Compound.the = Builder.Compound.CaffeineCompoundBuilder.get();

        float durFPS =
                fps;
        //fps * 2f; //nyquist

        RealTime clock =
                durFPS >= 10 / 2f ? /* nyquist threshold between decisecond (0.1) and centisecond (0.01) clock resolution */
                        new RealTime.CS(true) :
                        new RealTime.DSHalf(true);

        clock.durFPS(durFPS);

        Function<NAR, PrediTerm<Derivation>> deriver = Deriver.newDeriver(8
                , "motivation.nal");

        int THREADS = 3;
        NAR n = new NARS()
                .exe(
                        new MultiExec((i) ->
                                new MultiExec.Worker(
                                        new FocusExec() {
                                            @Override
                                            protected @Nullable
                                            NARService newTrigger() {
                                                return null;
                                            }
                                        }

//                                        new UniExec()

                                ), THREADS, 2))
                .time(clock)
                .index(
                        new CaffeineIndex(64 * 1024)
                        //new CaffeineIndex2(64 * 1024)
                        //new HijackTermIndex(Primes.nextPrime( 64 * 1024 + 1),  3)
                )
                .get();

        n.setEmotion(new Emotivation(n));

        n.confMin.setValue(0.01f);
        n.truthResolution.setValue(0.01f);
        n.termVolumeMax.setValue(30);

        n.beliefConfidence(0.9f);
        n.goalConfidence(0.9f);


        float priFactor = 0.1f;
        n.DEFAULT_BELIEF_PRIORITY = 1f * priFactor;
        n.DEFAULT_GOAL_PRIORITY = 1f * priFactor;
        n.DEFAULT_QUESTION_PRIORITY = 1f * priFactor;
        n.DEFAULT_QUEST_PRIORITY = 1f * priFactor;

        //n.dtDither.setValue(0.25f);
        //n.dtMergeOrChoose.setValue(true);

        STMLinkage stmLink = new STMLinkage(n, 1, false);
        MySTMClustered stmBelief = new MySTMClustered(n, 128, BELIEF, 3, true, 32f);
        //MySTMClustered stmBeliefAux = new MySTMClustered(n, 32, BELIEF, 4, true, 2f);
        //MySTMClustered stmGoal = new MySTMClustered(n, 96, GOAL, 3, true, 4f);
        Abbreviation abb = new Abbreviation(n, "z", 4, 9, 0.005f, 8);

        Inperience inp = new Inperience(n, 8);

        reflect.ReflectSimilarToTaskTerm refSim = new reflect.ReflectSimilarToTaskTerm(4, n);
        reflect.ReflectTaskClone refTask = new reflect.ReflectTaskClone(8, n);

        NAgent a = init.apply(n);
        //a.trace = true;


//        n.onTask(t -> {
//            if (t instanceof DerivedTask)
//                System.out.println(t);
//        });


        NInner nin = new NInner(n);
        nin.start();


        //init();


//        n.onCycle(nn -> {
//            float lag = narLoop.lagSumThenClear() + a.running().lagSumThenClear();
//            //n.emotion.happy(-lag);
//            //n.emotion.happy(n.emotion.busyPri.getSum()/50000f);
//        });


//        new Implier(a.nar,
//                Iterables.concat(
//                        Iterables.transform(a.actions.keySet(), ActionConcept::term),
//                        Collections.singleton(a.happy.term)
//                )
//        );

//        AgentService p = new AgentService.AgentBuilder(
//                //DQN::new,
//                HaiQAgent::new,
//                //() -> Util.tanhFast(a.dexterity())) //reward function
//                () -> a.dexterity() * Util.tanhFast( a.reward) /* - lag */ ) //reward function
//
//                .in(a::dexterity)
//                .in(new FloatNormalized(()->a.reward).decay(0.9f))
//                .in(new FloatNormalized(
//                        ((Emotivation) n.emotion).cycleDTRealMean::getValue)
//                            .decay(0.9f)
//                )
//                .in(new FloatNormalized(
//                        //TODO use a Long-specific impl of this:
//                        new FirstOrderDifferenceFloat(n::time, () -> n.emotion.taskDerived.getValue().longValue())
//                ).decay(0.9f))
//                .in(new FloatNormalized(
//                        //TODO use a Long-specific impl of this:
//                        new FirstOrderDifferenceFloat(n::time, () -> n.emotion.conceptFirePremises.getValue().longValue())
//                    ).decay(0.9f)
//                ).in(new FloatNormalized(
//                        () -> n.emotion.busyVol.getSum()
//                    ).decay(0.9f)
//                ).out(
//                        new StepController((x) -> n.time.dur(Math.round(x)), 1, n.dur(), n.dur()*2)
//                ).out(
//                        StepController.harmonic(n.confMin::setValue, 0.01f, 0.08f)
//                ).out(
//                        StepController.harmonic(n.truthResolution::setValue, 0.01f, 0.08f)
//                ).out(
//                        StepController.harmonic(a.curiosity::setValue, 0.01f, 0.16f)
//                ).get(n);

//
//        window(new MatrixView(p.in, (x, gl) -> {
//            Draw.colorBipolar(gl, x);
//            return 0;
//        }), 100, 100);


        //launch in separate thread cuz JOGL startup is slow
        new Thread(() -> {
            chart(a);
            chart(n, a);
            window( valueChart(a), 800, 600);
            window(/*row*/(

                    valuePlot(a)

                    //mixPlot(a, m, HISTORY),


                    //                        row(
                    ////                                new Plot2D(HISTORY, Plot2D.Line)
                    ////                                        .on(a::onFrame),
                    //                                new Plot2D(HISTORY, Plot2D.Line)
                    //                                        .add("happy", () -> m.lastScore)
                    //                                        .on(a::onFrame)
                    //                        )

                    ////                        new MatrixView(m.traffic, 4, (x, gl) -> {
                    ////                            Draw.colorGrays(gl, x);
                    ////                            return 0;
                    ////                        }),
                    //                        MatrixView.get((ArrayTensor) m.agentIn, 4, (x, gl) -> {
                    //                            Draw.colorGrays(gl, x);
                    //                            return 0;
                    //                        }),


                    ////                        new MatrixView(new RingBufferTensor(m.agentIn, 2), 2, (x, gl) -> {
                    ////                                    Draw.colorGrays(gl, x);
                    ////                                    return 0;
                    ////                                })


                    ////                        new MatrixView(((MultiHaiQMixAgent) m.agent).sharedPerception.W),
                    ////                        new MatrixView(((MultiHaiQMixAgent) m.agent).sharedPerception.y, false),
                    ////
                    ////                        row(
                    ////                                new MatrixView(((MultiHaiQMixAgent) m.agent).agent[0].q),
                    ////                                new MatrixView(((MultiHaiQMixAgent) m.agent).agent[1].q),
                    ////                                new MatrixView(((MultiHaiQMixAgent) m.agent).agent[2].q),
                    ////                                new MatrixView(((MultiHaiQMixAgent) m.agent).agent[3].q)
                    ////                        ),

                    // //new MatrixView(((MultiHaiQMixAgent)m.agent).agent[0].et),

                    //                        MatrixView.get(m.mixControl, 4, (x, gl) -> {
                    //                            Draw.colorBipolar(gl, (x - 0.5f) * 2f);
                    //                            return 0;
                    //                        })
            ), 600, 600);

            //get ready
            System.gc();

            NARLoop loop = a.nar.startFPS(fps);

        }).start();

        return n;
    }

    /**
     * increments/decrements within a finite set of powers-of-two so that harmonics
     * wont interfere as the resolution changes
     * <p>
     * TODO allow powers other than 2, ex: 1.618
     */
    public static class StepController implements IntConsumer, IntObjectPair<StepController> {

        private final FloatProcedure update;
        final float[] v;
        int x;

        public StepController(FloatProcedure update, float... steps) {
            v = steps;
            this.update = update;
        }

        public static StepController harmonic(FloatProcedure update, float min, float max) {

            FloatArrayList f = new FloatArrayList();
            float x = min;
            while (x <= max) {
                f.add(x);
                x *= 2;
            }
            assert (f.size() > 1);
            return new StepController(update, f.toArray());
            //set(0);
        }

        private void set(int i) {
            if (i < 0) i = 0;
            if (i >= v.length) i = v.length - 1;
            //if (this.x != i) {
            update.value(v[x = i]);
            //}
        }

        @Override
        public void accept(int aa) {
            //System.out.println(aa);

            switch (aa) {
                case 0:
                    set(x - 1);
                    break;
                case 1:
                    set(x + 1);
                    break;
                default:
                    throw new RuntimeException("OOB");
//                case 1:
//                    break; //nothing
            }
        }

        /**
         * number actions
         */
        @Override
        public int getOne() {
            return 2;
        }

        @Override
        public StepController getTwo() {
            return this;
        }

        @Override
        public int compareTo(@NotNull IntObjectPair<StepController> o) {
            throw new UnsupportedOperationException();
        }
    }


    private static Surface valueChart(NAgent a) {
        final Function<Cause,TreeChart.ItemVis<Cause>> builder = (i) -> {
            String str = i.toString();
            if (str.startsWith("class nars."))
                str = str.substring("class nars.".length()); //skip default toString

            if (str.startsWith("class "))
                str = str.substring(5); //skip default toString

            return new TreeChart.ItemVis<Cause>(i, StringUtils.abbreviate(str, 26)) {
                @Override
                public float requestedArea() {
                    return 0.1f + super.requestedArea();
                }
            };
        };
        return new TreeChart<Cause>() {
            final On<NAgent> on;
            {

                on = a.onFrame(() -> {
                    update(1, 1, a.nar.causes, (c, i) -> {
                        float v = c.value();
                        float r, g, b;
                        if (v < 0) {
                            r = 0.75f * Math.min(1f, -v); g = 0;
                        } else {
                            g = 0.75f * Math.min(1f, +v); r = 0;
                        }

                        float t = Util.sum(p->Math.abs(p.last), c.purpose);

                        b = Math.max(r, g) * t;

                        i.updateMomentum(
                                //0.01f + Util.sqr(Util.tanhFast(v)+1),
                                Math.signum(v) *(1+Math.abs(v))*(t), 0.05f,
                                r, g, b);

                    }, builder);
                });
            }

            @Override
            public void stop() {
                super.stop();
                on.off();
            }
        };
    }
    private static Surface valuePlot(NAgent a) {
        NAR nar = a.nar;

        int s = nar.causes.size();

        FloatParam gain = new FloatParam(1f, 0f, 10f);

        BitmapMatrixView bmp = new BitmapMatrixView((i) ->
                gain.floatValue() * nar.causes.get(i).value(),
                s, Math.max(1, (int) Math.sqrt(s)),
                Draw::colorBipolar) {

            final On<NAgent> on;

            {
                on = a.onFrame(() -> {
                    update();
                });
            }

            @Override
            public void stop() {
                super.stop();
                on.off();
            }
        };

        return bmp;
    }

    private static void chart(NAR n, NAgent a) {
        window(new NARSView(n, a), 600, 600);
    }

    public static class NARSView extends Grid {


        public NARSView(NAR n, NAgent a) {
            super(
                    //new MixBoard(n, n.in),
                    //new MixBoard(n, n.nalMix), //<- currently dont use this it will itnerfere with the stat collection
                    Vis.reflect(n),
                    new Vis.EmotionPlot(64, a)
                    //row(n.sub.stream().map(c -> Vis.reflect(n)).collect(toList()))
            );
//                (n.sub.stream().map(c -> {
//                int capacity = 128;
//                return new BagChart<ITask>(
//                        //new Bagregate<>(
//                                ((BufferedSynchronousExecutorHijack) c.exe).active
//                          //      ,capacity*2,
//                            //    0.9f
//                        //)
//                        ,capacity) {
//
//                    @Override
//                    public void accept(ITask x, ItemVis<ITask> y) {
//                        float p = Math.max(x.priElseZero(), Pri.EPSILON);
//                        float r = 0, g = 0, b = 0;
//                        int hash = x.hashCode();
//                        switch (Math.abs(hash) % 3) {
//                            case 0: r = p/2f; break;
//                            case 1: g = p/2f; break;
//                            case 2: b = p/2f; break;
//                        }
//                        switch (Math.abs(2837493 ^ hash) % 3) {
//                            case 0: r += p/2f; break;
//                            case 1: g += p/2f; break;
//                            case 2: b += p/2f; break;
//                        }
//
//                        y.update(p, r, g, b);
//                    }
//                };
//            }).collect(toList()))); //, 0.5f);
            a.onFrame(x -> update());
        }

        protected void update() {
//            /*bottom().*/forEach(x -> {
//                x.update();
//            });
        }
    }

    //    public static NAR newAlann(int dur) {
//
//        NAR nar = NARBuilder.newALANN(new RealTime.CS(true).dur( dur ), 3, 512, 3, 3, 2 );
//
//        nar.termVolumeMax.set(32);
//
//        MySTMClustered stm = new MySTMClustered(nar, 64, '.', 8, true, 3);
//        MySTMClustered stmGoal = new MySTMClustered(nar, 32, '!', 8, true, 3);
//
////        Abbreviation abbr = new Abbreviation(nar, "the",
////                4, 16,
////                0.05f, 32);
//
//        new Inperience(nar, 0.05f, 16);
//
//        /*SpaceGraph.window(grid(nar.cores.stream().map(c ->
//                Vis.items(c.activeBag(), nar, 16)).toArray(Surface[]::new)), 900, 700);*/
//
//        return nar;
//    }


    public static void chart(NAgent a) {
        NAR nar = a.nar;
        a.nar.runLater(() -> {
            window(grid(

                    grid(
                            new WindowButton("nar", () -> nar),
                            new WindowButton("emotion", () -> Vis.emotionPlots(a, 256))
                            //new WindowButton( "focus", nar::focus),

                            //new WindowButton("mix", () -> new MixBoard(nar, nar.in))
                    ),

                    grid(
                            new WindowButton("log", () -> Vis.logConsole(nar, 80, 25, new FloatParam(0f))),
                            new WindowButton("top", () -> (new ConsoleTerminal(new nars.TextUI(nar).session(20f)))),
                            new WindowButton("prompt", () -> Vis.newInputEditor(), 300, 60)
                    ),

                    grid(
                            Vis.beliefCharts(16, nar, a.reward),
                            new WindowButton("agent", () -> (a)),
                            new WindowButton("actionShort", () -> Vis.beliefCharts(a.nar.dur() * 4, a.actions.keySet(), a.nar)),
                            new WindowButton("actionMed", () -> Vis.beliefCharts(a.nar.dur() * 32, a.actions.keySet(), a.nar)),
                            new WindowButton("actionLong", () -> Vis.beliefCharts(a.nar.dur() * 64, a.actions.keySet(), a.nar)),
                            new WindowButton("predict", () -> Vis.beliefCharts(200, a.predictors, a.nar)),
                            //"agentActions",
                            //"agentPredict",

                            a instanceof NAgentX ?
                                    new WindowButton("vision", () -> grid(((NAgentX) a).cam.stream().map(cs ->
                                            new CameraSensorView(cs, a).align(Surface.Align.Center, cs.width, cs.height))
                                            .toArray(Surface[]::new))
                                    ) : grid()
                    ),

                    grid(
//                    new WindowButton( "conceptBudget",
//                            ()->{
//
//                                double[] d = new double[32];
//                                return new HistogramChart(
//                                        ()->d,
//                                        //()->h.uniformProb(32, 0, 1.0)
//                                        new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.25f)) {
//
//                                    On on = a.onFrame((r) -> {
//                                        Bag.priHistogram(r.nar.focus().concepts(), d);
//                                    });
//
//                                    @Override
//                                    public Surface hide() {
//                                        on.off();
//                                        return this;
//                                    }
//                                };
//                            }
//                        //Vis.budgetHistogram(nar, 64)
//                    ),

//                    new WindowButton( "conceptTreeMap", () -> {
//
//                        BagChart tc = new Vis.ConceptBagChart(new Bagregate(
//                                ((NARS)a.nar).sub.stream().flatMap(x ->
//                                        (((BufferedSynchronousExecutorHijack)(x.exe)).active.stream().map(
//                                    y -> (y instanceof ConceptFire) ? ((ConceptFire)y) : null
//                                ).filter(Objects::nonNull)), 128, 0.5f), 128, nar);
//
//                        return tc;
//                    })

                            //"tasks", ()-> taskChart,

                            new WindowButton("conceptGraph", () ->
                                    Vis.conceptsWindow3D(nar, 128, 4))

                    )
            ), 900, 600);
        });
    }

//    public static void chart(NAgent a) {
//
//        a.nar.runLater(() -> {
//
//            //Vis.conceptsWindow3D(a.nar, 64, 12).show(1000, 800);
////
////            BagChart<Concept> tc = new Vis.ConceptBagChart(new Bagregate(a.nar.focus().concepts(), 32, 0.5f), 32, a.nar);
////
//
//            window(
//                    grid(
//                            new ReflectionSurface<>(a),
//                            Vis.beliefCharts(100, a.actions, a.nar ),
//
//                            Vis.emotionPlots(a, 256),
//
//                            //tc,
//
//
//                            //budgetHistogram(d, 16),
//
//                            //Vis.agentActions(a, 50),
//                            //Vis.beliefCharts(400, a.predictors, a.nar),
//                            new ReflectionSurface<>(a.nar),
//
//                            Vis.budgetHistogram(a.nar, 24)
//                            /*Vis.conceptLinePlot(nar,
//                                    Iterables.concat(a.actions, Lists.newArrayList(a.happy, a.joy)),
//                                    2000)*/
//                    ), 1200, 900);
//        });
//    }

    /**
     * pixelTruth defaults to linear monochrome brightness -> frequency
     */
    protected CameraSensor senseCamera(String id, Container w, int pw, int ph) throws Narsese.NarseseException {
        return senseCamera(id, new SwingBitmap2D(w), pw, ph);
    }

    protected CameraSensor<Scale> senseCamera(String id, Supplier<BufferedImage> w, int pw, int ph) throws Narsese.NarseseException {
        return senseCamera(id, new Scale(w, pw, ph));
    }

//    protected CameraSensor<Scale> senseCamera(String id, Container w, int pw, int ph) throws Narsese.NarseseException {
//        return senseCamera(id, new Scale(new SwingBitmap2D(w), pw, ph));
//    }

    protected Sensor2D<PixelBag> senseCameraRetina(String id, Container w, int pw, int ph) throws Narsese.NarseseException {
        return senseCameraRetina(id, new SwingBitmap2D(w), pw, ph);
    }

    protected Sensor2D<PixelBag> senseCameraRetina(String id, Container w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) throws Narsese.NarseseException {
        return senseCameraRetina(id, new SwingBitmap2D(w), pw, ph);
    }

    protected CameraSensor<PixelBag> senseCameraRetina(String id, Supplier<BufferedImage> w, int pw, int ph) throws Narsese.NarseseException {
        return senseCameraRetina($(id), w, pw, ph);
    }

    protected CameraSensor<PixelBag> senseCameraRetina(Term id, Supplier<BufferedImage> w, int pw, int ph) {
        PixelBag pb = PixelBag.of(w, pw, ph);
        pb.addActions(id, this);
        return senseCamera(id, pb);
    }

    protected Sensor2D<WaveletBag> senseCameraFreq(String id, Supplier<BufferedImage> w, int pw, int ph) throws Narsese.NarseseException {
        WaveletBag pb = new WaveletBag(w, pw, ph);
        return senseCamera(id, pb);
    }

    protected <C extends Bitmap2D> CameraSensor<C> senseCamera(@Nullable String id, C bc) throws Narsese.NarseseException {
        return senseCamera(id != null ? $(id) : null, bc);
    }

    protected <C extends Bitmap2D> CameraSensor<C> senseCamera(@Nullable Term id, C bc) {
        return addCamera(new CameraSensor(id, bc, this));
    }

    protected <C extends Bitmap2D> CameraSensor<C> senseCameraReduced(@Nullable Term id, C bc, int outputPixels) {
        return addCamera(new CameraSensor(id, new AutoencodedBitmap(bc, outputPixels), this));
    }

    protected <C extends Bitmap2D> CameraSensor<C> addCamera(CameraSensor<C> c) {
        cam.add(c);
        return c;
    }

    static Surface mixPlot(NAgent a, MixContRL m, int history) {
        return Grid.grid(m.dim, i -> col(
                new MixGainPlot(a, m, history, i),
                new MixTrafficPlot(a, m, history, i)
        ));
    }

    private static class MixGainPlot extends Plot2D {
        public MixGainPlot(NAgent a, MixContRL m, int history, int i) {
            super(history, BarWave);

            add(m.id(i), () -> m.gain(i), -1f, +1f);
            a.onFrame(this::update);
        }
    }

    private static class MixTrafficPlot extends Plot2D {
        public MixTrafficPlot(NAgent a, MixContRL m, int history, int i) {
            super(history, Line);
            add(m.id(i) + "_in", () -> m.trafficInput(i), 0f, 1f);
            add(m.id(i), () -> m.trafficActive(i), 0f, 1f);
            a.onFrame(this::update);
        }
    }

    //    private static class CorePanel extends Surface{
//
//        public CorePanel(Default2.GraphPremiseBuilder c, NAR nar) {
//            super();
//            grid(Vis.items(c.terms, nar, 10))
//        }
//    }

//    protected <C extends PixelCamera> MatrixSensor addMatrixAutoEncoder(String id, C bc, FloatToObjectFunction<Truth> pixelTruth) {
//        CameraSensor c = new CameraSensor<>($.the(id), bc, this, pixelTruth);
//        cam.put(id, c);
//        return c;
//    }

}

