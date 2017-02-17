package nars.remote;

import jcog.bag.PLink;
import jcog.data.FloatParam;
import jcog.data.random.XorShift128PlusRandom;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.Task;
import nars.attention.Forget;
import nars.bag.impl.ArrayBag;
import jcog.bag.Bag;
import nars.bag.Bagregate;
import nars.bag.impl.BLinkHijackBag;
import nars.budget.BudgetMerge;
import nars.concept.Concept;
import nars.conceptualize.DefaultConceptBuilder;
import nars.gui.BagChart;
import nars.gui.Vis;
import nars.index.term.map.CaffeineIndex;
import nars.budget.BLink;
import nars.budget.RawBLink;
import nars.nar.Default;
import nars.op.Leak;
import nars.op.mental.Compressor;
import nars.op.mental.Inperience;
import nars.op.stm.MySTMClustered;
import nars.term.Term;
import nars.time.FrameTime;
import nars.time.RealTime;
import nars.time.Time;
import nars.truth.Truth;
import nars.util.exe.Executioner;
import nars.util.exe.MultiThreadExecutioner;
import nars.util.task.TaskStatistics;
import nars.video.*;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import spacegraph.Surface;
import spacegraph.space.layout.Grid;
import spacegraph.space.layout.TabPane;
import spacegraph.space.widget.CheckBox;
import spacegraph.space.widget.FloatSlider;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.$.t;
import static spacegraph.SpaceGraph.window;
import static spacegraph.space.layout.Grid.grid;

/**
 * Created by me on 9/19/16.
 */
abstract public class NAgents extends NAgent {

    public final Map<String, CameraSensor> cam = new LinkedHashMap<>();

    public NAgents(NAR nar) {
        this("", nar);
    }

    public NAgents(String id, NAR nar) {
        this(id, nar, 1);
    }

    public NAgents(String id, NAR nar, int reasonerFramesPerEnvironmentFrame) {
        super(id, nar, reasonerFramesPerEnvironmentFrame);
    }

    public static void run(Function<NAR, NAgents> init, int frames) {
        Default nar = newMultiThreadNAR(3, new FrameTime(), true);
        //Default nar = newNAR();
        //Default2 nar = newNAR2();

        NAgents a = init.apply(nar);
        a.trace = true;


        chart(a);

        a.run(frames);

        print(nar, a);


        //((TreeTaskIndex)nar.tasks).tasks.prettyPrint(System.out);

    }

    private static void print(NAR nar, NAgents a) {
        //NAR.printActiveTasks(nar, true);
        //NAR.printActiveTasks(nar, false);

        nar.forEachTask(x -> {
            System.out.println(x);
            //if (x.isQuestOrQuestion())
                ///System.out.println(x.proof());
        });

        nar.printConceptStatistics();
        new TaskStatistics().add(nar).print(System.out);

        a.predictors.forEach(p->{
            Concept pp = nar.concept(p);
            if (pp!=null)
                pp.print();
        });
    }

    public static NAR runRT(Function<NAR, NAgents> init, float fps) {
        return runRT(init, fps, 1, -1);
    }

    public static NAR runRT(Function<NAR, NAgents> init, float fps, int durFrames, int endTime) {

        Time clock = new RealTime.CS(true).dur(durFrames / fps);
        NAR nar =
                //new TaskNAR(32 * 1024, new MultiThreadExecutioner(4, 4 * 1024), clock);
                NAgents.newMultiThreadNAR(4, clock, false);
        //NAR nar = newNAR();
        //NAR nar = newAlann(durFrames/fps);

        NAgents a = init.apply(nar);
        a.trace = true;

        chart(a);

        a.runRT(fps, endTime).join();

        print(nar, a);

        return nar;


    }

    private static Default newMultiThreadNAR(int cores, Time time, boolean sync) {
        Default d = newMultiThreadNAR(cores, time);
        ((MultiThreadExecutioner) d.exe).sync(sync);
        return d;
    }

//    public static NAR newAlann(float dur) {
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


    public static Default newMultiThreadNAR(int threads, Time time) {
        Random rng = new XorShift128PlusRandom(1);
        final Executioner exe =
                //new SingleThreadExecutioner();
                new MultiThreadExecutioner(threads, 16384 /* TODO chose a power of 2 number to scale proportionally to # of threads */)
                    //.sync(false)
                ;

        int conceptsPerCycle = 48*threads;

        final int reprobes = 4;

        //Multi nar = new Multi(3,512,
        DefaultConceptBuilder cb = new DefaultConceptBuilder() {
            /*@Override
            public <X> X withBags(Term t, BiFunction<Bag<Term, BLink<Term>>, Bag<Task, BLink<Task>>, X> f) {
                Bag<Term, BLink<Term>> termlink = new BLinkHijackBag(reprobes, BudgetMerge.orBlend, rng );
                Bag<Task, BLink<Task>> tasklink = new BLinkHijackBag(reprobes, BudgetMerge.orBlend, rng );
                return f.apply(termlink, tasklink);
            }*/
        };

        Default nar = new Default(16 * 1024,
                conceptsPerCycle, 1, 3, rng,
                new CaffeineIndex(cb, 64*1024, false, exe)
                //new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 300000, 32 * 1024, 3)
                ,
                time,
                exe) {

            final Compressor compressor = new Compressor(this, "_", 2, 8,
                    1f, 16, 256);

            @Override
            public Task pre(@NotNull Task t) {
                if (!t.isInput() ) {
                    return compressor.encode(t);
                } else {
                    return t; //dont affect input
                }
            }

            @NotNull
            @Override
            public Term pre(@NotNull Term t) {
                return compressor.encode(t);
            }

            @NotNull
            @Override public Task post(@NotNull Task t) {
                return compressor.decode(t);
            }

            @Override
            @NotNull public Term post(@NotNull Term t) {
                return compressor.decode(t);
            }

//            @Override
//            protected BLinkHijackBag<Concept> newConceptBag(int activeConcepts) {
//                return new BLinkHijackBag<>(activeConcepts, reprobes, BudgetMerge.plusBlend, random ) {
//                    @Override
//                    public Forget forget(float rate) {
//                        float memoryForget = 0.98f;
//                        return new Forget(rate, memoryForget, memoryForget);
//                    }
//                };
//            }
        };

        nar.beliefConfidence(0.75f);
        nar.goalConfidence(0.75f);

        float p = 0.75f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.5f * p;
        nar.DEFAULT_GOAL_PRIORITY = 0.6f * p;
        nar.DEFAULT_QUESTION_PRIORITY = 0.4f * p;
        nar.DEFAULT_QUEST_PRIORITY = 0.4f * p;

        nar.confMin.setValue(0.05f);
        nar.truthResolution.setValue(0.05f);
        nar.termVolumeMax.setValue(72);

        MySTMClustered stm = new MySTMClustered(nar, 64, '.', 3, true, 6);
        MySTMClustered stmGoal = new MySTMClustered(nar, 32, '!', 2, true, 4);

//        Abbreviation abbr = new Abbreviation(nar, "the",
//                4, 16,
//                0.02f, 32);

        new Inperience(nar, 0.02f, 16);

//        //causal accelerator
//        nar.onTask(t -> {
//
//            switch (t.op()) {
//                case IMPL:
//                    //decompose with Goal:Induction
//                    if (t.isBelief()) {
//                        Term subj = t.term(0);
//                        Term pred = t.term(1);
//                        if (pred instanceof Compound && (subj.vars() == 0) && (pred.vars() == 0)) {
//                            Concept postconditionConcept = nar.concept(pred);
//
//                            //if (pred.equals(a1.term()) || pred.equals(a2.term())) {
//                            boolean negate = false;
//                            if (subj.op() == NEG) {
//                                subj = subj.unneg();
//                                negate = true;
//                            }
//                            Concept preconditionConcept = nar.concept(subj);
//                            if (preconditionConcept != null) {
//
//                                int dt = t.dt();
//                                if (dt == DTERNAL)
//                                    dt = 0;
//
//                                for (long when : new long[]{t.occurrence(),
//                                        nar.time(), nar.time() + 1, nar.time() + 2 //, nar.time() + 200, nar.time() + 300}
//                                }) {
//
//                                    if (when == ETERNAL)
//                                        continue;
//
//                                    //TODO project, not just eternalize for other times
//                                    Truth tt = when != t.occurrence() ? t.truth().eternalize() : t.truth();
//
//                                    if (!(postconditionConcept instanceof SensorConcept)) {
//                                        {
//                                            Task preconditionBelief = preconditionConcept.beliefs().top(when);
//                                            if (preconditionBelief != null) {
//                                                Truth postcondition = BeliefFunction.Deduction.apply(preconditionBelief.truth().negated(negate), tt, nar, nar.confMin.floatValue());
//                                                if (postcondition != null) {
//                                                    Task m = new GeneratedTask(pred, '.', postcondition.truth())
//                                                            .evidence(Stamp.zip(t, preconditionBelief))
//                                                            .budget(t.budget())
//                                                            .time(nar.time(), when + dt)
//                                                            .log("Causal Accel");
//                                                    nar.inputLater(m);
//                                                }
//                                            }
//                                        }
//                                        {
//                                            Task preconditionGoal = preconditionConcept.goals().top(when);
//                                            if (preconditionGoal != null) {
//                                                Truth postcondition = GoalFunction.Induction.apply(preconditionGoal.truth().negated(negate), tt, nar, nar.confMin.floatValue());
//                                                if (postcondition != null) {
//                                                    Task m = new GeneratedTask(pred, '!', postcondition.truth())
//                                                            .evidence(Stamp.zip(t, preconditionGoal))
//                                                            .budget(t.budget())
//                                                            .time(nar.time(), when + dt)
//                                                            .log("Causal Accel");
//                                                    nar.inputLater(m);
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                            //}
//                        }
//                    }
//                    break;
//            }
//        });


        return nar;
    }

    public static void chart(NAgents a) {
        NAR nar = a.nar;

//        BagChart<Task> taskChart = new BagChart<Task>(new Leak<Task,PLink<Task>>(new ArrayBag<Task>(16, BudgetMerge.maxBlend, new ConcurrentHashMap<>()), 0f, a.nar) {
//
//            @Override
//            protected float onOut(@NotNull PLink<Task> b) {
//                return 1;
//            }
//
//            @Override
//            protected void in(@NotNull Task task, Consumer<PLink<Task>> each) {
//                if (!task.isCommand() && !task.isDeleted())
//                    each.accept(new RawBLink<>(task, task, 0.1f));
//            }
//
//        }.bag, 16);
//        a.nar.onCycle(f -> taskChart.update());

        a.nar.runLater(()-> {

            //Vis.conceptsWindow2D(a.nar, Iterables.concat(a.predictors, a.actions, a.sensors) /* a.nar */,64 ,8).show(1000, 800);
            //Vis.conceptsWindow2D(a.nar, 16 ,4).show(1000, 800);
//
//            window( new Widget(new TileTab(Maps.mutable.of(
//                "x", () -> new PushButton("x"),
//                "y", () -> new PushButton("y"),
//                "dsf", () -> grid(new Label("y"), new Label("xy"), new Label("xyzxcv"))
//            ))), 800, 600);
            window(
                    new TabPane(new TreeMap<String,Supplier<Surface>>(Map.of(
                            "agent", ()-> new ReflectionSurface(a),
                            //"control", () -> new ReflectionSurface(a.nar),
                            "input", () -> grid(a.cam.values().stream().map(cs ->
                                    new CameraSensorView(cs, nar).align(Surface.Align.Center, cs.width, cs.height))
                                    .toArray(Surface[]::new)),
                            "inputEdit",()->Vis.newInputEditor(a.nar),
//                            "concepts", ()->
//                                    Vis.treeChart( a.nar, new Bagregate(a.nar.conceptsActive(), 64, 0.05f) , 64),
                            "conceptBudget", ()->
                                    Vis.budgetHistogram(nar, 64),
                            //"tasks", ()-> taskChart,
                            "agentCharts", ()-> Vis.emotionPlots(a.nar, 256),
                            "agentActions", ()-> Vis.agentActions(a, 400),
                            "agentPredict", ()-> Vis.beliefCharts(400, a.predictors, a.nar)

                    ))

                            //nar instanceof Default ? Vis.concepts((Default) nar, 128) : grid(/*blank*/),


                            /*Vis.conceptLinePlot(nar,
                                    Iterables.concat(a.actions, Lists.newArrayList(a.happy, a.joy)),
                                    2000)*/
                    ), 1200, 900);
        });
    }
    public static void chart(NAgent a) {

        a.nar.runLater(()-> {

            Vis.conceptsWindow3D(a.nar, 64, 12).show(1000, 800);

            window(
                    grid(
                            new ReflectionSurface<>(a),

                            Vis.emotionPlots(a.nar, 256),


                            //conceptsTreeChart(d, count),
                            //budgetHistogram(d, 16),

                            Vis.agentActions(a, 400),
                            Vis.beliefCharts(400, a.predictors, a.nar),
                            new ReflectionSurface<>(a.nar),

                            Vis.budgetHistogram(a.nar, 24)
                            /*Vis.conceptLinePlot(nar,
                                    Iterables.concat(a.actions, Lists.newArrayList(a.happy, a.joy)),
                                    2000)*/
                    ), 1200, 900);
        });
    }

    /**
     * pixelTruth defaults to linear monochrome brightness -> frequency
     */
    protected Sensor2D senseCamera(String id, Container w, int pw, int ph) {
        return senseCamera(id, w, pw, ph, (v) -> t(v, alpha));
    }

    protected Sensor2D<Scale> senseCamera(String id, Container w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        return senseCamera(id, new Scale(new SwingCamera(w), pw, ph), pixelTruth);
    }

    protected Sensor2D<PixelBag> senseCameraRetina(String id, Container w, int pw, int ph) {
        return senseCameraRetina(id, new SwingCamera(w), pw, ph, (v) -> t(v, alpha));
    }

    protected Sensor2D<PixelBag> senseCameraRetina(String id, Container w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        return senseCameraRetina(id, new SwingCamera(w), pw, ph, pixelTruth);
    }

    protected Sensor2D<PixelBag> senseCameraRetina(String id, Supplier<BufferedImage> w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        PixelBag pb = PixelBag.of(w, pw, ph);
        pb.addActions(id, this);
        return senseCamera(id, pb, pixelTruth);
    }

    protected Sensor2D<WaveletBag> addFreqCamera(String id, Supplier<BufferedImage> w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        WaveletBag pb = new WaveletBag(w, pw, ph);
        return senseCamera(id, pb, pixelTruth);
    }

    protected <C extends Bitmap2D> Sensor2D<C> senseCamera(String id, C bc, FloatToObjectFunction<Truth> pixelTruth) {
        CameraSensor c = new CameraSensor<>($.the(id), bc, this, pixelTruth);
        cam.put(id, c);
        return c;
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

    public static class ReflectionSurface<X> extends Grid {

        private final X x;

        public ReflectionSurface(@NotNull X x)  {
            this.x = x;

            List<Surface> l = $.newArrayList();


            Class cc = x.getClass();
            for (Field f : cc.getFields()) {
            //SuperReflect.fields(x, (String k, Class c, SuperReflect v) -> {

                try {
                    String k = f.getName();
                    Class c = f.getType();

                    if (c == FloatParam.class) {
                        FloatParam p = (FloatParam) f.get(x);
                        l.add(col(Vis.label(k), new FloatSlider(p) ));
                    } else if (c == AtomicBoolean.class) {
                        AtomicBoolean p = (AtomicBoolean) f.get(x);
                        l.add(new CheckBox(k, p));
                    }
                    /*else {
                        l.add(new PushButton(k));
                    }*/
                }catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            setChildren(l);
        }
    }

}
