package nars;

import nars.bag.Bagregate;
import nars.concept.Concept;
import nars.gui.Vis;
import nars.nar.Default;
import nars.nar.NARBuilder;
import nars.time.RealTime;
import nars.time.Time;
import nars.truth.Truth;
import nars.util.task.TaskStatistics;
import nars.video.*;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import spacegraph.Surface;
import spacegraph.space.layout.TabPane;
import spacegraph.space.widget.ReflectionSurface;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.$.t;
import static spacegraph.SpaceGraph.window;
import static spacegraph.space.layout.Grid.grid;

/**
 * Extensions to NAgent interface:
 *
 *      --chart output (spacegraph)
 *      --cameras (Swing and OpenGL)
 */
abstract public class NAgentX extends NAgent {

    public final Map<String, CameraSensor> cam = new LinkedHashMap<>();

    public NAgentX(NAR nar) {
        this("", nar);
    }

    public NAgentX(String id, NAR nar) {
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

    private static void print(NAR nar, NAgentX a) {
        //NAR.printActiveTasks(nar, true);
        //NAR.printActiveTasks(nar, false);

        nar.forEachTask(x -> {
            System.out.println(x);
            //if (x.isQuestOrQuestion())
            ///System.out.println(x.proof());
        });

        nar.printConceptStatistics();
        new TaskStatistics().add(nar).print(System.out);

    }

    public static NAR runRT(Function<NAR, NAgentX> init, float fps) {
        return runRT(init, fps, 1, -1);
    }

    public static NAR runRT(Function<NAR, NAgentX> init, float fps, int durFrames, int endTime) {

        Time clock = new RealTime.DSHalf(true).dur(durFrames / fps);
        NAR nar =
                //new TaskNAR(32 * 1024, new MultiThreadExecutioner(4, 4 * 1024), clock);
                NARBuilder.newMultiThreadNAR(4, clock, true);
        //NAR nar = newNAR();
        //NAR nar = newAlann(durFrames/fps);

        NAgentX a = init.apply(nar);
        a.trace = true;

        chart(a);

        a.runRT(fps, endTime).join();

        return nar;


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


    public static void chart(NAgentX a) {
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

        a.nar.runLater(() -> {

            //Vis.conceptsWindow2D(a.nar, 64, 12).show(1000, 800);

            //Vis.conceptsWindow2D(a.nar, Iterables.concat(a.predictors, a.actions, a.sensors) /* a.nar */,64 ,8).show(1000, 800);
            //Vis.conceptsWindow2D(a.nar, 16 ,4).show(1000, 800);
//
//            window( new Widget(new TileTab(Maps.mutable.of(
//                "x", () -> new PushButton("x"),
//                "y", () -> new PushButton("y"),
//                "dsf", () -> grid(new Label("y"), new Label("xy"), new Label("xyzxcv"))
//            ))), 800, 600);
            window(
                    new TabPane(new TreeMap<String, Supplier<Surface>>(Map.of(
                            "agent", () -> new ReflectionSurface(a),
                            "core", () -> new ReflectionSurface(((Default)a.nar).core),
                            "nar", () -> new ReflectionSurface(a.nar),
                            "input", () -> grid(a.cam.values().stream().map(cs ->
                                    new CameraSensorView(cs, nar).align(Surface.Align.Center, cs.width, cs.height))
                                    .toArray(Surface[]::new)),
                            "inputEdit", () -> Vis.newInputEditor(a.nar),
                            "concepts", ()->
                                    Vis.treeChart( a.nar, new Bagregate(a.nar.conceptsActive(), 64, 0.05f) , 64),
                            "conceptBudget", () ->
                                    Vis.budgetHistogram(nar, 64),
                            //"tasks", ()-> taskChart,
                            "agentCharts", () -> Vis.emotionPlots(a.nar, 256),
                            "agentActions", () -> Vis.agentActions(a, 400)
                            ///"agentPredict", () -> Vis.beliefCharts(400, a.predictors, a.nar)

                    ))

                            //nar instanceof Default ? Vis.concepts((Default) nar, 128) : grid(/*blank*/),


                            /*Vis.conceptLinePlot(nar,
                                    Iterables.concat(a.actions, Lists.newArrayList(a.happy, a.joy)),
                                    2000)*/
                    ), 1200, 900);
        });
    }

    public static void chart(NAgent a) {

        a.nar.runLater(() -> {

            Vis.conceptsWindow3D(a.nar, 64, 12).show(1000, 800);

            window(
                    grid(
                            new ReflectionSurface<>(a),

                            Vis.emotionPlots(a.nar, 256),


                            //conceptsTreeChart(d, count),
                            //budgetHistogram(d, 16),

                            Vis.agentActions(a, 400),
                            //Vis.beliefCharts(400, a.predictors, a.nar),
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
        return senseCamera(id, w, pw, ph, (v) -> t(v, alpha()));
    }

    protected CameraSensor<Scale> senseCamera(String id, Supplier<BufferedImage> w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        return senseCamera(id, new Scale(w, pw, ph), pixelTruth);
    }

    protected Sensor2D<Scale> senseCamera(String id, Container w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        return senseCamera(id, new Scale(new SwingCamera(w), pw, ph), pixelTruth);
    }

    protected Sensor2D<PixelBag> senseCameraRetina(String id, Container w, int pw, int ph) {
        return senseCameraRetina(id, new SwingCamera(w), pw, ph, (v) -> t(v, alpha()));
    }

    protected Sensor2D<PixelBag> senseCameraRetina(String id, Container w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        return senseCameraRetina(id, new SwingCamera(w), pw, ph, pixelTruth);
    }

    protected CameraSensor<PixelBag> senseCameraRetina(String id, Supplier<BufferedImage> w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        PixelBag pb = PixelBag.of(w, pw, ph);
        pb.addActions(id, this);
        return senseCamera(id, pb, pixelTruth);
    }

    protected Sensor2D<WaveletBag> senesCameraFreq(String id, Supplier<BufferedImage> w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        WaveletBag pb = new WaveletBag(w, pw, ph);
        return senseCamera(id, pb, pixelTruth);
    }

    protected <C extends Bitmap2D> CameraSensor<C> senseCamera(String id, C bc, FloatToObjectFunction<Truth> pixelTruth) {
        CameraSensor<C> c = new CameraSensor<>($.the(id), bc, this, pixelTruth);
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

}
