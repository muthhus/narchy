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
import nars.nar.Multi;
import nars.nar.exe.Executioner;
import nars.nar.exe.MultiThreadExecutioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.mental.Abbreviation;
import nars.op.mental.Inperience;
import nars.op.time.MySTMClustered;
import nars.time.RealtimeMSClock;
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
import java.util.stream.Stream;

import static nars.$.t;
import static spacegraph.SpaceGraph.window;
import static spacegraph.obj.GridSurface.grid;

/**
 * Created by me on 9/19/16.
 */
abstract public class NAgents extends NAgent {

    public final Map<String, CameraSensor> cam = new LinkedHashMap<>();

    public NAgents(NAR nar, int reasonerFramesPerEnvironmentFrame) {
        super(nar, reasonerFramesPerEnvironmentFrame);

    }

    public static void runRT(Function<NAR, NAgents> init) {


        //Default nar = newNAR();
        Default nar = newNAR1async(4);
        //Default2 nar = newNAR2();



        MySTMClustered stm = new MySTMClustered(nar, 64, '.', 3, true, 8);
        MySTMClustered stmGoal = new MySTMClustered(nar, 32, '!', 2, true, 4);

        Abbreviation abbr = new Abbreviation(nar, "the",
                4, 8,
                0.01f, 8);

        new Inperience(nar);

        NAgents a = init.apply(nar);
        a.trace = true;


        chart(a);


        //a.run(frames);
        a.runRT(60f).join();

        NAR.printTasks(nar, true);
        NAR.printTasks(nar, false);

        nar.tasks.forEach(x -> {
            if (x.isQuestOrQuestion())
                System.out.println(x.proof());
        });

        nar.printConceptStatistics();

        //((TreeTaskIndex)nar.tasks).tasks.prettyPrint(System.out);
    }

    private static Default newNAR1async(int cores) {
        Default d = newMultiThreadNAR(cores);
        ((MultiThreadExecutioner)d.exe).sync(false);
        return d;
    }

    private static Default2 newNAR2() {
        Default2 d = new Default2();

        SpaceGraph.window(grid( d.cores.stream().map(c ->
                Vis.items(c.terms, d, 16)).toArray(Surface[]::new) ), 900, 700);

        return d;
    }

    private static Default newNAR3(int cores) {
        Multi m = new Multi(cores, (i, j) -> {
            //feedforward
            if (i+1 == j )
                return 0.9f; //decay

            //if ((i + 1) % cores == j)
               // return 0.9f / (j - i);

            return 0;
            //return Math.random() < 0.5f ? 0.8f : 0f;
        });

        Default in = m.core[0];

        SpaceGraph.window(grid( Stream.of(m.core).map(c ->
                Vis.items(c.core.active, c, 32)).toArray(Surface[]::new) ), 900, 700);

        return in;
    }



    public static Default newMultiThreadNAR(int threads) {
        Random rng = new XorShift128PlusRandom(1);
        final Executioner exe =
                //new SingleThreadExecutioner();
                new MultiThreadExecutioner(threads, 8192 /* TODO chose a power of 2 number to scale proportionally to # of threads */);

        int volMax = 48;
        int conceptsPerCycle = 128;


        //Multi nar = new Multi(3,512,
        Default nar = new Default(2048,
                conceptsPerCycle, 2, 3, rng,
                //new CaffeineIndex(new DefaultConceptBuilder(rng), 1024*1024, volMax/2, false, exe)
                new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 4* 1024 * 128, 16*1024, 4)

                ,
                //new FrameClock()
                new RealtimeMSClock(true),
                exe) {

            @Override
            protected void initNAL7() {
                //no STM linkage
            }
        };


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.8f);

        float p = 0.15f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.9f * p;
        nar.DEFAULT_GOAL_PRIORITY = 1f * p;
        nar.DEFAULT_QUESTION_PRIORITY = 0.7f * p;
        nar.DEFAULT_QUEST_PRIORITY = 0.8f * p;

        nar.confMin.setValue(0.04f);
        nar.compoundVolumeMax.setValue(volMax);

        nar.linkFeedbackRate.setValue(0.05f);
        return nar;
    }

    public static void chart(NAgents a) {
        NAR nar = a.nar;
        window(
                grid(
                        grid(a.cam.values().stream().map(cs -> new CameraSensorView(cs, nar)).toArray(Surface[]::new)),

                        Vis.concepts((Default)nar, 128),

                        Vis.agentActions(a, 200),

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