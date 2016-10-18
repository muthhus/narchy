package nars.remote;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.gui.Vis;
import nars.index.term.tree.TreeTermIndex;
import nars.nar.Default;
import nars.nar.Default2;
import nars.nar.exe.Executioner;
import nars.nar.exe.MultiThreadExecutioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.mental.Abbreviation;
import nars.op.mental.Inperience;
import nars.op.time.MySTMClustered;
import nars.time.FrameClock;
import nars.truth.Truth;
import nars.util.data.random.XorShift128PlusRandom;
import nars.video.*;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import spacegraph.SpaceGraph;
import spacegraph.Surface;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.$.t;
import static spacegraph.SpaceGraph.window;
import static spacegraph.obj.GridSurface.grid;

/**
 * Created by me on 9/19/16.
 */
abstract public class SwingAgent extends NAgent {

    public final Map<String, CameraSensor> cam = new LinkedHashMap<>();

    public SwingAgent(NAR nar, int reasonerFramesPerEnvironmentFrame) {
        super(nar, reasonerFramesPerEnvironmentFrame);

    }

    public static void run(Function<NAR, SwingAgent> init, int frames) {


        //Default nar = newNAR();
        Default2 nar = newNAR2();


        MySTMClustered stm = new MySTMClustered(nar, 64, '.', 3, true);
        MySTMClustered stmGoal = new MySTMClustered(nar, 32, '!', 2, true);

        Abbreviation abbr = new Abbreviation(nar, "the",
                4, 8,
                0.005f, 8);

        new Inperience(nar);

        SwingAgent a = init.apply(nar);
        a.trace = true;

        int history = 200;
        chart(a, history);


        a.run(frames);

        NAR.printTasks(nar, true);
        NAR.printTasks(nar, false);

        nar.tasks.forEach(x -> {
            if (x.isQuestOrQuestion())
                System.out.println(x.proof());
        });

        nar.printConceptStatistics();

        //((TreeTaskIndex)nar.tasks).tasks.prettyPrint(System.out);
    }

    private static Default2 newNAR2() {
        Default2 d = new Default2();

        SpaceGraph.window(grid( d.cores.stream().map(c ->
                Vis.items(c.terms, d, 8)).toArray(Surface[]::new) ), 900, 700);

        return d;
    }

    public static Default newNAR() {
        Random rng = new XorShift128PlusRandom(4);
        final Executioner exe =
                //new SingleThreadExecutioner();
                new MultiThreadExecutioner(3, 1024*8);

        int volMax = 40;
        int conceptsPerCycle = 64;


        //Multi nar = new Multi(3,512,
        Default nar = new Default(1024,
                conceptsPerCycle, 2, 3, rng,
                //new CaffeineIndex(new DefaultConceptBuilder(rng), 1024*1024, volMax/2, false, exe)
                new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 400000, 64*1024, 3)

                , new FrameClock(), exe);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.9f);

        float p = 0.1f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.9f * p;
        nar.DEFAULT_GOAL_PRIORITY = 1f * p;
        nar.DEFAULT_QUESTION_PRIORITY = 0.7f * p;
        nar.DEFAULT_QUEST_PRIORITY = 0.8f * p;

        nar.confMin.setValue(0.02f);
        nar.compoundVolumeMax.setValue(volMax);

        nar.linkFeedbackRate.setValue(0.05f);
        return nar;
    }

    public static void chart(SwingAgent a, int history) {
        NAR nar = a.nar;
        window(
                grid(
                        grid(a.cam.values().stream().map(cs -> new CameraSensorView(cs, nar)).toArray(Surface[]::new)),

                        //Vis.concepts(nar, 512),

                        Vis.agentActions(a, 200),

                        //Vis.budgetHistogram(nar, 32),
                        Vis.conceptLinePlot(nar,
                                Iterables.concat(a.actions, Lists.newArrayList(a.happy, a.joy)),
                                2000)
                ), 1200, 900);
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

    protected Sensor2D<PixelBag> addCameraRetina(String id, Container w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        return addCamera(id, new SwingCamera(w), pw, ph, pixelTruth);
    }

    protected Sensor2D<PixelBag> addCamera(String id, Supplier<BufferedImage> w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
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


}
