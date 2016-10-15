package nars.remote;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.googlecode.concurrenttrees.common.PrettyPrinter;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.gui.Vis;
import nars.index.task.TreeTaskIndex;
import nars.index.term.tree.TreeTermIndex;
import nars.nar.Default;
import nars.nar.exe.Executioner;
import nars.nar.exe.MultiThreadExecutioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.mental.Abbreviation;
import nars.op.time.MySTMClustered;
import nars.time.FrameClock;
import nars.truth.Truth;
import nars.util.data.random.XorShift128PlusRandom;
import nars.video.*;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
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
        Random rng = new XorShift128PlusRandom(1);

        final Executioner exe =
            //new SingleThreadExecutioner();
            new MultiThreadExecutioner(3, 1024*4);

        int volMax = 32;
        int conceptsPerCycle = 32;

        //Multi nar = new Multi(3,512,
        Default nar = new Default(2048,
                conceptsPerCycle, 2, 3, rng,
                //new CaffeineIndex(new DefaultConceptBuilder(rng), 1024*1024, volMax/2, false, exe)
                new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(new XorShift128PlusRandom(3)), 400000, 64*1024, 3)

                , new FrameClock(), exe);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.8f);

        float p = 0.02f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.75f * p;
        nar.DEFAULT_GOAL_PRIORITY = 1f * p;
        nar.DEFAULT_QUESTION_PRIORITY = 0.25f * p;
        nar.DEFAULT_QUEST_PRIORITY = 0.5f * p;

        nar.confMin.setValue(0.05f);
        nar.compoundVolumeMax.setValue(volMax);

        //nar.linkFeedbackRate.setValue(0.05f);


        MySTMClustered stm = new MySTMClustered(nar, 128, '.', 3, true);
        MySTMClustered stmGoal = new MySTMClustered(nar, 32, '!', 2, true);

        Abbreviation abbr = new Abbreviation(nar, "the",
                4, 8,
                0.5f, 64);

        SwingAgent a = init.apply(nar);
        a.trace = true;


        int history = 200;
        chart(a, history);


        a.run(frames).join();
        //a.runSync(runFrames);

        NAR.printTasks(nar, true);
        NAR.printTasks(nar, false);

        nar.printConceptStatistics();

        //nar.tasks.forEach(System.out::println);
        //((TreeTaskIndex)nar.tasks).tasks.prettyPrint(System.out);
    }

    public static void chart(SwingAgent a, int history) {
        Default nar = (Default)a.nar;
        window(
                grid(
                        grid(a.cam.values().stream().map(cs -> new CameraSensorView(cs, nar)).toArray(Surface[]::new)),

                        //Vis.concepts(nar, 512),

                        Vis.agentActions(a, history*a.frameRate),

                        Vis.budgetHistogram(nar, 32),
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
//    protected <C extends PixelCamera> MatrixSensor addMatrixAutoEncoder(String id, C bc, FloatToObjectFunction<Truth> pixelTruth) {
//        CameraSensor c = new CameraSensor<>($.the(id), bc, this, pixelTruth);
//        cam.put(id, c);
//        return c;
//    }


}
