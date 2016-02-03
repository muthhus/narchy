package nars.rover.run;

import com.gs.collections.impl.factory.Lists;
import javafx.scene.layout.VBox;
import javassist.scopedpool.SoftValueHashMap;
import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.guifx.NARfx;
import nars.nar.Default;
import nars.rover.RoverWorld;
import nars.rover.Sim;
import nars.rover.robot.NARover;
import nars.rover.world.FoodSpawnWorld1;
import nars.term.Termed;
import nars.term.index.MapIndex2;
import nars.time.SimulatedClock;
import nars.util.signal.hai;

import java.util.Collections;
import java.util.List;


/**
 * Created by me on 6/20/15.
 */
public class SomeRovers {

    public static final String motorLeft = "MotorControls(left,motor,(),#z)";
    public static final String motorRight = "MotorControls(right,motor,(),#z)";
    public static final String motorForward = "MotorControls(forward,motor,(),#z)";
    ////"MotorControls(backward,motor,(),#z)",
    public static final String motorStop = "MotorControls(stop,motor,(),#z)";

    public static final String eatFood = "eat:food";
    public static final String eatPoison = "eat:poison";
    public static final String speedLeft = "speed:left";
    public static final String speedRight = "speed:right";
    public static final String speedForward = "speed:forward";

    private static final SimulatedClock clock = new SimulatedClock();

    public static void main(String[] args) {

        Global.DEBUG = Global.EXIT_ON_EXCEPTION = true;


        //RoverWorld world = new ReactorWorld(32, 48, 32);

        RoverWorld world = new FoodSpawnWorld1(256, 48, 48, 0.5f);

        //RoverWorld world = new GridSpaceWorld(GridSpaceWorld.newMazePlanet());


        final Sim game = new Sim(clock, world);


//        game.add(new Turret("turret"));
//
//        game.add(new Spider("spider",
//                3, 3, 0.618f, 30, 30));


        boolean addNARRover = true;
        boolean addQRover = false;

        if (addNARRover) {
            game.add(q(new NARover("r1", newNAR())));
        }


        if (addQRover) {
            game.add(new QRover("r2"));
        }

//        {
//            NAR nar = new Default();
//
//            //nar.param.outputVolume.set(0);
//
//            game.add(new CarefulRover("r2", nar));
//        }

        float fps = 30;
        game.run(fps);

    }

    public static Default newNAR() {
        int conceptsFirePerCycle = 2;
        Default nar = new Default(
                new Memory(clock, new MapIndex2(
                        new SoftValueHashMap())),
                1200, conceptsFirePerCycle, 2, 4);

//            nar.memory.DEFAULT_JUDGMENT_PRIORITY = 0.35f;
//            nar.memory.DEFAULT_JUDGMENT_DURABILITY = 0.35f;
//            nar.memory.DEFAULT_GOAL_PRIORITY = 0.7f;
//            nar.memory.DEFAULT_GOAL_DURABILITY = 0.7f;
//            nar.memory.DEFAULT_QUESTION_PRIORITY = 0.6f;
//            nar.memory.DEFAULT_QUESTION_DURABILITY = 0.6f;

        //nar.initNAL9();
        //nar.memory.the(new Anticipate(nar));


        //nar.memory.perfection.setValue(0.15f);
        nar.core.confidenceDerivationMin.setValue(0.01f);

        //nar.core.activationRate.setValue(1f / conceptsFirePerCycle /* approxmimate */);
        nar.core.activationRate.setValue(0.5f);

        nar.memory.duration.set(5);
        nar.memory.conceptForgetDurations.setValue(3);
        nar.memory.termLinkForgetDurations.setValue(10);
        nar.memory.taskLinkForgetDurations.setValue(20);
        nar.memory.cyclesPerFrame.set(64);
        nar.memory.shortTermMemoryHistory.set(4);
        //nar.memory.executionExpectationThreshold.setValue(0.95f);


        boolean gui = true;
        if (gui) {
            //NARide.loop(nar, false);

            NARfx.run( () -> {
//                    NARide.newIDE(nar.loop(), (i) -> {
//
//                    }, new Stage());

                NARfx.newConceptWindow(nar,
                        //new TilePane(Orientation.VERTICAL),
                        new VBox(),
                        "MotorControls(#x,motor,(),#z)",
                        motorLeft,
                        motorRight,
                        motorForward,

                        motorStop
                );

                NARfx.newConceptWindow(nar,
                        //new TilePane(Orientation.VERTICAL),
                        new VBox(),
                        eatFood,
                        eatPoison,
                        speedLeft,
                        speedRight,
                        speedForward
                        //"speed:backward"
                );
            });
        }

        return nar;
    }

    /** attaches a prosthetic q-controller to a NAR */
    public static NARover q(NARover r) {
        NAR n = r.nar;
        hai q = new hai(n);

        List<Termed> sensors = Global.newArrayList();
        Collections.addAll(sensors, n.terms(eatFood, eatPoison, speedLeft, speedRight, speedForward));

        q.set(
            sensors,
            Lists.mutable.of(n.term("eat:food")),
            Lists.mutable.of(n.term(motorForward), n.term(motorLeft), n.term(motorRight), n.term(motorStop))
        );



        return r;
    }

//    private static class InputActivationController extends CycleReaction {
//
//        private final NAR nar;
//
//        final int windowSize;
//
//        final DescriptiveStatistics busyness;
//
//        public InputActivationController(NAR nar) {
//            super(nar);
//            this.nar = nar;
//            this.windowSize = nar.memory.duration();
//            this.busyness = new DescriptiveStatistics(windowSize);
//        }
//
//        @Override
//        public void onCycle() {
//
//            final float bInst = nar.memory.emotion.busy();
//            busyness.addValue(bInst);
//
////            float bAvg = (float)busyness.getMean();
//
////            float busyMax = 3f;
//
////            double a = nar.memory.inputActivationFactor.get();
////            if (bAvg > busyMax) {
////                a -= 0.01f;
////            }
////            else  {
////                a += 0.01f;
////            }
////
////            final float min = 0.01f;
////            if (a < min) a = min;
////            if (a > 1f) a = 1f;
////
////            //System.out.println("act: " + a + " (" + bInst + "," + bAvg);
////
////            nar.param.inputActivationFactor.set(a);
////            nar.param.conceptActivationFactor.set( 0.5f * (1f + a) /** half as attenuated */ );
//        }
//    }

}
