package nars.remote;

import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.Task;
import nars.bag.impl.ArrayBag;
import nars.bag.impl.Bagregate;
import nars.budget.merge.BudgetMerge;
import nars.gui.BagChart;
import nars.gui.Vis;
import nars.index.term.tree.TreeTermIndex;
import nars.link.BLink;
import nars.link.DefaultBLink;
import nars.nar.Alann;
import nars.nar.Default;
import nars.nar.Multi;
import nars.nar.exe.Executioner;
import nars.nar.exe.MultiThreadExecutioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.Leak;
import nars.op.mental.Abbreviation;
import nars.op.mental.Inperience;
import nars.op.time.MySTMClustered;
import nars.time.FrameTime;
import nars.time.RealTime;
import nars.time.Time;
import nars.truth.Truth;
import nars.util.TaskStatistics;
import nars.util.data.FloatParam;
import nars.util.data.random.XorShift128PlusRandom;
import nars.video.*;
import objenome.O;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.layout.Grid;
import spacegraph.obj.layout.TabPane;
import spacegraph.obj.widget.CheckBox;
import spacegraph.obj.widget.FloatSlider;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static nars.$.t;
import static spacegraph.SpaceGraph.window;
import static spacegraph.obj.layout.Grid.grid;

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

        NAR.printActiveTasks(nar, true);
        NAR.printActiveTasks(nar, false);

//        nar.tasks.forEach(x -> {
//            if (x.isQuestOrQuestion())
//                System.out.println(x.proof());
//        });

        nar.printConceptStatistics();
        new TaskStatistics().add(nar).print(System.out);

        a.predictors.forEach(p->{
            nar.concept(p).print();
        });


        //((TreeTaskIndex)nar.tasks).tasks.prettyPrint(System.out);

    }

    public static void runRT(Function<NAR, NAgents> init) {
        runRT(init, 10);
    }

    public static void runRT(Function<NAR, NAgents> init, float fps) {
        runRT(init, fps, 1);
    }

    public static void runRT(Function<NAR, NAgents> init, float fps, int durFrames) {

        Default nar = NAgents.newMultiThreadNAR(3, new RealTime.CS(true).dur(durFrames/fps), true);
        //Default nar = newNAR();
        //Alann nar = newAlann();

        NAgents a = init.apply(nar);
        a.trace = true;
        chart(a);

        a.runRT(fps).join();



    }

    private static Default newMultiThreadNAR(int cores, Time time, boolean sync) {
        Default d = newMultiThreadNAR(cores, time);
        ((MultiThreadExecutioner) d.exe).sync(sync);
        return d;
    }

    public static Alann newAlann() {
        Alann nar = new Alann(new RealTime.DS(true));

        MySTMClustered stm = new MySTMClustered(nar, 128, '.', 3, true, 6);
        MySTMClustered stmGoal = new MySTMClustered(nar, 32, '!', 2, true, 4);

        Abbreviation abbr = new Abbreviation(nar, "the",
                4, 16,
                0.05f, 32);

        new Inperience(nar, 0.05f);

        SpaceGraph.window(grid(nar.cores.stream().map(c ->
                Vis.items(c.terms, nar, 16)).toArray(Surface[]::new)), 900, 700);

        return nar;
    }

    private static Default newNAR3(int cores) {
        Multi m = new Multi(cores, (i, j) -> {
            //feedforward
            if (i + 1 == j)
                return 0.9f; //decay

            //if ((i + 1) % cores == j)
            // return 0.9f / (j - i);

            return 0;
            //return Math.random() < 0.5f ? 0.8f : 0f;
        });

        Default in = m.core[0];

        SpaceGraph.window(grid(Stream.of(m.core).map(c ->
                Vis.items(c.core.active, c, 32)).toArray(Surface[]::new)), 900, 700);

        return in;
    }


    public static Default newMultiThreadNAR(int threads, Time time) {
        Random rng = new XorShift128PlusRandom(1);
        final Executioner exe =
                //new SingleThreadExecutioner();
                new MultiThreadExecutioner(threads, 16384 /* TODO chose a power of 2 number to scale proportionally to # of threads */);

        int volMax = 28;
        int conceptsPerCycle = 24*threads;


        //Multi nar = new Multi(3,512,
        Default nar = new Default(2048,
                conceptsPerCycle, 2, 4, rng,
                //new CaffeineIndex(new DefaultConceptBuilder(rng), 1024*1024, volMax/2, false, exe)
                new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 40000, 32 * 1024, 2)

                ,
                //new FrameClock()
                time,
                exe) {

//            @Override
//            protected void initNAL7() {
//                //no STM linkage
//            }
        };

        nar.beliefConfidence(0.75f);
        nar.goalConfidence(0.75f);

        float p = 0.5f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.75f * p;
        nar.DEFAULT_GOAL_PRIORITY = 1f * p;
        nar.DEFAULT_QUESTION_PRIORITY = 0.5f * p;
        nar.DEFAULT_QUEST_PRIORITY = 0.5f * p;

        nar.confMin.setValue(0.01f);
        nar.termVolumeMax.setValue(volMax);

        MySTMClustered stm = new MySTMClustered(nar, 64, '.', 3, true, 6);
        MySTMClustered stmGoal = new MySTMClustered(nar, 32, '!', 2, true, 4);

        Abbreviation abbr = new Abbreviation(nar, "the",
                4, 16,
                0.01f, 32);

        new Inperience(nar, 0.02f);

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

        BagChart<Task> taskChart = new BagChart<Task>(new Leak<Task>(new ArrayBag<Task>(16, BudgetMerge.plusBlend, new ConcurrentHashMap<>()), 0f, a.nar) {


            @Override
            protected float onOut(@NotNull BLink<Task> b) {
                return 0;
            }

            @Override
            protected void in(@NotNull Task task, Consumer<BLink<Task>> each) {
                if (!task.isDeleted())
                    each.accept(new DefaultBLink<>(task, task, 0.1f));
            }

        }.bag, 16);
        a.nar.onFrame(f -> taskChart.update());

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
                    new TabPane(Map.of(
                            "control", () -> new ReflectionSurface(a.nar),
                            "input", () -> grid(a.cam.values().stream().map(cs ->
                                    new CameraSensorView(cs, nar).align(Surface.Align.Center, cs.width, cs.height))
                                    .toArray(Surface[]::new)),
                            "inputEdit",()->Vis.newInputEditor(a.nar),
                            "concepts", ()->
                                    Vis.treeChart( a.nar, new Bagregate(((Default)a.nar).core.active, 64, 0.05f, 256) , 64),
                            "conceptBudget", ()->
                                    Vis.budgetHistogram(nar, 24),
                            "tasks", ()-> taskChart,
                            "agent", ()-> Vis.emotionPlots(a.nar, 256),
                            "agentControl", ()-> new ReflectionSurface(a),
                            "agentActions", ()-> Vis.agentActions(a, 400),
                            "agentPredict", ()-> Vis.beliefCharts(400, a.predictors, a.nar)

                    )

                            //nar instanceof Default ? Vis.concepts((Default) nar, 128) : grid(/*blank*/),


                            /*Vis.conceptLinePlot(nar,
                                    Iterables.concat(a.actions, Lists.newArrayList(a.happy, a.joy)),
                                    2000)*/
                    ), 1200, 900);
        });
    }
    public static void chart(NAgent a) {

        a.nar.runLater(()-> {

            Vis.conceptsWindow3D(a.nar, 128, 6).show(1000, 800);

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
    protected Sensor2D addCamera(String id, Container w, int pw, int ph) {
        return addCamera(id, w, pw, ph, (v) -> t(v, alpha));
    }

    protected Sensor2D<Scale> addCamera(String id, Container w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        return addCamera(id, new Scale(new SwingCamera(w), pw, ph), pixelTruth);
    }

    protected Sensor2D<PixelBag> addCameraRetina(String id, Container w, int pw, int ph) {
        return addCameraRetina(id, new SwingCamera(w), pw, ph, (v) -> t(v, alpha));
    }

    protected Sensor2D<PixelBag> addCameraRetina(String id, Container w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        return addCameraRetina(id, new SwingCamera(w), pw, ph, pixelTruth);
    }

    protected Sensor2D<PixelBag> addCameraRetina(String id, Supplier<BufferedImage> w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        PixelBag pb = new PixelBag(w, pw, ph);
        pb.addActions(id, this);
        return addCamera(id, pb, pixelTruth);
    }

    protected Sensor2D<WaveletBag> addFreqCamera(String id, Supplier<BufferedImage> w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        WaveletBag pb = new WaveletBag(w, pw, ph);
        return addCamera(id, pb, pixelTruth);
    }

    protected <C extends Bitmap2D> Sensor2D<C> addCamera(String id, C bc, FloatToObjectFunction<Truth> pixelTruth) {
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

        public ReflectionSurface(X x) {
            this.x = x;

            List<Surface> l = $.newArrayList();
            O.in(x).fields((k,c,v) -> {

                if (c == FloatParam.class) {
                    FloatParam f = v.get();
                    l.add(col(Vis.label(k), new FloatSlider(f) ));
                } else if (c == AtomicBoolean.class) {
                    l.add(new CheckBox(k, v.get()));
                }
                /*else {
                    l.add(new PushButton(k));
                }*/
            });
            setChildren(l);
        }
    }

}
