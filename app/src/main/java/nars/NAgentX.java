package nars;

import jcog.data.FloatParam;
import nars.bag.Bagregate;
import nars.gui.Vis;
import nars.nar.Default;
import nars.nar.NARBuilder;
import nars.term.Term;
import nars.time.RealTime;
import nars.time.Time;
import nars.truth.Truth;
import nars.util.task.TaskStatistics;
import nars.video.*;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import spacegraph.Surface;
import spacegraph.layout.TabPane;
import spacegraph.widget.meta.ReflectionSurface;
import spacegraph.widget.meta.WindowButton;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.$.t;
import static spacegraph.SpaceGraph.window;
import static spacegraph.layout.Grid.grid;

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

    public static NAR runRT(Function<NAR, NAgent> init, float fps) {
        return runRT(init, fps, 1, -1);
    }

    public static NAR runRT(Function<NAR, NAgent> init, float fps, int durFrames, int endTime) {

        Time clock = new RealTime.DSHalf(true).durSeconds(durFrames / fps);
        NAR nar =
                //new TaskNAR(32 * 1024, new MultiThreadExecutioner(4, 4 * 1024), clock);
                NARBuilder.newMultiThreadNAR(4, clock, true);
        //NAR nar = newNAR();
        //NAR nar = newAlann(durFrames/fps);

        NAgent a = init.apply(nar);
        //a.trace = true;

        if (a instanceof NAgentX)
            chart((NAgentX)a);
        else
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
        a.nar.runLater(() -> {
            window( grid(
                new WindowButton( "nar", () -> a.nar ),
                new WindowButton( "agent", () -> (a) ),
                new WindowButton( "deriver", () -> ((Default)a.nar).derivationBudgeting ),
                new WindowButton( "focus", () -> (((Default)a.nar).core) ),
                new WindowButton( "log", () -> Vis.logConsole(nar, 90, 40, new FloatParam(0f)) ),
                new WindowButton( "vision", () -> grid(a.cam.values().stream().map(cs ->
                                    new CameraSensorView(cs, nar).align(Surface.Align.Center, cs.width, cs.height))
                                    .toArray(Surface[]::new))
                                ),
                new WindowButton("input", () -> Vis.newInputEditor(a.nar)),

                grid(
                    new WindowButton( "emotion", () -> Vis.emotionPlots(a.nar, 256) ),
                            //"agentActions", () -> Vis.agentActions(a, 64f)
                            //"agentPredict", () -> Vis.beliefCharts(400, a.predictors, a.nar)
                    new WindowButton( "conceptBudget", Vis.budgetHistogram(nar, 64) ),
                    new WindowButton( "conceptTree", ()-> Vis.treeChart( a.nar, new Bagregate(a.nar.conceptsActive(), 64, 0.5f) , 64) ),
                            //"tasks", ()-> taskChart,
                    new WindowButton( "conceptGraph", ()-> Vis.conceptsWindow3D(a.nar, 32, 4).show(500,500) )
                )
            ), 1200, 900);
        });
    }

    public static void chart(NAgent a) {

        a.nar.runLater(() -> {

            //Vis.conceptsWindow3D(a.nar, 64, 12).show(1000, 800);

            window(
                    grid(
                            new ReflectionSurface<>(a),

                            Vis.emotionPlots(a.nar, 256),

                            Vis.treeChart( a.nar, new Bagregate(a.nar.conceptsActive(), 32, 0.05f) , 32),


                            //budgetHistogram(d, 16),

                            Vis.agentActions(a, 50),
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
    protected CameraSensor senseCamera(String id, Container w, int pw, int ph) {
        return senseCamera(id, w, pw, ph, (v) -> t(v, alpha()));
    }

    protected CameraSensor<Scale> senseCamera(String id, Supplier<BufferedImage> w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        return senseCamera(id, new Scale(w, pw, ph), pixelTruth);
    }

    protected CameraSensor<Scale> senseCamera(String id, Container w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
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
        return senseCamera(id, new CameraSensor<>($.the(id), bc, nar, pixelTruth));
    }

    protected <C extends Bitmap2D> CameraSensor<C> senseCamera(String id, CameraSensor<C> c) {
        sense(c);
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
