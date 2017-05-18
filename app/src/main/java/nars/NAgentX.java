package nars;

import jcog.bag.Bag;
import jcog.bag.util.Bagregate;
import jcog.data.FloatParam;
import jcog.event.On;
import nars.concept.Concept;
import nars.gui.BagChart;
import nars.gui.HistogramChart;
import nars.gui.MixBoard;
import nars.gui.Vis;
import nars.nar.Default;
import nars.nar.NARBuilder;
import nars.term.Term;
import nars.time.RealTime;
import nars.time.Time;
import nars.truth.Truth;
import nars.video.*;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import spacegraph.Surface;
import spacegraph.math.Color3f;
import spacegraph.widget.meta.ReflectionSurface;
import spacegraph.widget.meta.WindowButton;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

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

        int durFrames = 1;

        Time clock = new RealTime.
                DSHalf(true)
                //CS(true)
                .durSeconds((durFrames) / fps);
        Default nar =
                NARBuilder.newMultiThreadNAR(-1, clock, true);

        //NAR nar = newNAR();
        //NAR nar = newAlann(durFrames/fps);

        NAgent a = init.apply(nar);
        //a.trace = true;

        if (a instanceof NAgentX)
            chart((NAgentX)a);
        else
            chart(a);


        a.runRT(fps, endTime);

        return nar;
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


    public static void chart(NAgentX a) {
        NAR nar = a.nar;
        a.nar.runLater(() -> {
            window( grid(

                grid(
                    new WindowButton( "nar", () -> nar ),
                    new WindowButton( "emotion", () -> Vis.emotionPlots(a, 256) ),
                    new WindowButton( "focus", nar::focus),
                    new WindowButton( "deriver", () -> (((Default)nar).deriver) ),
                    new WindowButton( "deriverFilter", () -> ((Default)nar).derivationBudgeting ),
                    new WindowButton( "mix", () -> {
                        Default d = (Default) nar;
                        return new MixBoard(d, d.mix);
                    })
                ),

                grid(
                    new WindowButton( "log", () -> Vis.logConsole(nar, 80, 25, new FloatParam(0f)) ),
                    new WindowButton("prompt", () -> Vis.newInputEditor(), 300, 60)
                ),

                grid(
                    Vis.beliefCharts(16, nar, a.happy),
                    new WindowButton( "agent", () -> (a) ),
                    new WindowButton( "action", () -> Vis.beliefCharts(100, a.actions, a.nar ) ),
                    new WindowButton( "predict", () -> Vis.beliefCharts(100, a.predictors, a.nar ) ),
                        //"agentActions",
                        //"agentPredict",

                    new WindowButton( "vision", () -> grid(a.cam.values().stream().map(cs ->
                                        new CameraSensorView(cs, a).align(Surface.Align.Center, cs.width, cs.height))
                                        .toArray(Surface[]::new))
                                )
                ),

                grid(
                    new WindowButton( "conceptBudget",
                            ()->{

                                double[] d = new double[32];
                                return new HistogramChart(
                                        ()->d,
                                        //()->h.uniformProb(32, 0, 1.0)
                                        new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.25f)) {

                                    On on = a.onFrame((r) -> {
                                        Bag.priHistogram(r.nar.focus().concepts(), d);
                                    });

                                    @Override
                                    public Surface hide() {
                                        on.off();
                                        return this;
                                    }
                                };
                            }
                        //Vis.budgetHistogram(nar, 64)
                    ),
                    new WindowButton( "conceptTreeMap", () -> {

                        BagChart<Concept> tc = new Vis.ConceptBagChart(new Bagregate(a.nar.focus().concepts(), 128, 0.5f), 128, nar);

                        return tc;
                    }),
                            //"tasks", ()-> taskChart,
                    new WindowButton( "conceptGraph", ()->
                            Vis.conceptsWindow3D(nar,128, 6) )
                )
            ), 900, 600);
        });
    }

    public static void chart(NAgent a) {

        a.nar.runLater(() -> {

            //Vis.conceptsWindow3D(a.nar, 64, 12).show(1000, 800);

            BagChart<Concept> tc = new Vis.ConceptBagChart(new Bagregate(a.nar.focus().concepts(), 32, 0.5f), 32, a.nar);


            window(
                    grid(
                            new ReflectionSurface<>(a),

                            Vis.emotionPlots(a, 256),

                            tc,


                            //budgetHistogram(d, 16),

                            //Vis.agentActions(a, 50),
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
        PixelBag pb = PixelBag.of(w, pw, ph);
        pb.addActions(id, this);
        return senseCamera(id, pb);
    }

    protected Sensor2D<WaveletBag> senseCameraFreq(String id, Supplier<BufferedImage> w, int pw, int ph) throws Narsese.NarseseException {
        WaveletBag pb = new WaveletBag(w, pw, ph);
        return senseCamera(id, pb);
    }

    protected <C extends Bitmap2D> CameraSensor<C> senseCamera(String id, C bc) throws Narsese.NarseseException {
        return senseCamera($.$(id), bc);
    }

    protected <C extends Bitmap2D> CameraSensor<C> senseCamera(Term id, C bc) {
        return senseCamera(id.toString(), new CameraSensor( id, bc, this ));
    }

    protected <C extends Bitmap2D> CameraSensor<C> senseCamera(String id, CameraSensor<C> c) {
        //sense(c);

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
