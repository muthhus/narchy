package nars.rover.run;

import javafx.scene.layout.VBox;
import javassist.scopedpool.SoftValueHashMap;
import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.Symbols;
import nars.guifx.NARfx;
import nars.nar.Default;
import nars.op.mental.Anticipate;
import nars.op.mental.Inperience;
import nars.rover.RoverWorld;
import nars.rover.Sim;
import nars.rover.robot.Arm;
import nars.rover.robot.NARover;
import nars.rover.world.FoodSpawnWorld1;
import nars.term.index.MapIndex2;
import nars.time.SimulatedClock;
import nars.op.sys.NarQ;
import nars.op.sys.NarQ.BeliefReward;
import nars.op.sys.NarQ.InputTask;
import nars.op.sys.NarQ.NotBeliefReward;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jbox2d.common.Vec2;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by me on 6/20/15.
 */
public class SomeRovers {

	public static final String motorLeft = "MotorControls(left,motor,(),#z)";
	public static final String motorRight = "MotorControls(right,motor,(),#z)";
	public static final String motorForward = "MotorControls(forward,motor,(),#z)";
	public static final String motorBackward = "MotorControls(backward,motor,(),#z)";
	public static final String motorStop = "MotorControls(stop,motor,(),#z)";
    public static final String fire = "MotorControls(fire,motor,(),#z)";

	public static final String eatFood = "eat:food";
	public static final String eatPoison = "eat:poison";
	public static final String speedAngular = "speed:angular";
	public static final String speedLinear = "speed:forward";

	public static final SimulatedClock clock = new SimulatedClock();

	public static void main(String[] args) {

        Global.DEBUG = false;
        Global.EXIT_ON_EXCEPTION = true;

        //RoverWorld world = new ReactorWorld(32, 48, 32);
        RoverWorld world = new FoodSpawnWorld1(64, 48, 48, 0.5f);

        //RoverWorld world = new GridSpaceWorld(GridSpaceWorld.newMazePlanet());
        final Sim game = new Sim(clock, world);

        //game.add(new Turret("turret"));

//        game.add(new Spider("spider",
//                3, 3, 0.618f, 30, 30));
        boolean addNARRover = true;
        boolean addQRover = true;

        if (addNARRover) {
            game.add(new NARover("r1", newNAR()) {
                @Override
                public void init(Sim p) {
                    super.init(p);

                    q(this);
                }

            });
        }

//        if (addQRover) {
//            game.add(new QRover("r2"));
//        }

//        {
//            NAR nar = new Default();
//
//            //nar.param.outputVolume.set(0);
//
//            game.add(new CarefulRover("r2", nar));
//        }
        float fps = 40;
        game.run(fps);

    }
	public static Default newNAR() {
        int conceptsFirePerCycle = 32;
        Default nar = new Default(
                new Memory(clock, new MapIndex2(
                        new SoftValueHashMap())),
                2000, conceptsFirePerCycle, 3, 4);

        //nar.logSummaryGT(System.out, 0.2f);
//        nar.log(System.out, x -> {
//            if (x instanceof Task) {
//                Task t = (Task)x;
//                //if (t.isInput()) return false;
//                return t.budget().summary() > 0.0f;
//            }
//            return false;
//        });

        @NotNull Memory m = nar.memory;
        m.DEFAULT_JUDGMENT_PRIORITY = 0.4f;
//            nar.memory.DEFAULT_JUDGMENT_DURABILITY = 0.35f;
        m.DEFAULT_GOAL_PRIORITY = 0.6f;
//            nar.memory.DEFAULT_GOAL_DURABILITY = 0.7f;
        m.DEFAULT_QUESTION_PRIORITY = 0.4f;

//            nar.memory.DEFAULT_QUESTION_DURABILITY = 0.6f;
        //nar.initNAL9();
        m.the(new Anticipate(nar));
        m.the(new Inperience(nar));
        //nar.memory.perfection.setValue(0.15f);
        //nar.core.confidenceDerivationMin.setValue(0.01f);


        //nar.core.activationRate.setValue(1f / conceptsFirePerCycle /* approxmimate */);
        nar.core.activationRate.setValue(0.8f);


        m.duration.set(2);
        m.conceptForgetDurations.setValue(1f);
        m.termLinkForgetDurations.setValue(3);
        m.taskLinkForgetDurations.setValue(8);
        m.cyclesPerFrame.set(2);
        m.shortTermMemoryHistory.set(3);
        m.executionThreshold.setValue(0.0f);

        boolean gui = true;
        if (gui) {
            //NARide.loop(nar, false);

            NARfx.run(() -> {
//                    NARide.newIDE(nar.loop(), (i) -> {
//
//                    }, new Stage());

//
                NARfx.newConceptWindow(nar,
                        //new TilePane(Orientation.VERTICAL),
                        new VBox(),
                        "MotorControls(#x,motor,(),#z)",
                        fire,
                        motorLeft,
                        motorRight,
                        motorForward,
                        motorBackward,
                        motorStop
                );

                NARfx.newConceptWindow(nar,
                        //new TilePane(Orientation.VERTICAL),
                        new VBox(),
                        eatFood,
                        eatPoison,
                        speedAngular,
                        speedLinear
                );
            });
        }

        return nar;
    }


	/**
	 * attaches a prosthetic q-controller to a NAR
	 */
	public static void q(NARover r) {
        NAR n = r.nar;


        NarQ nqSpine = new NarQ(n, (i,o) -> (int) Math.ceil(Math.sqrt(i * o)));


        nqSpine.power.setValue(0.8f);


        nqSpine.input.addAll(nqSpine.getBeliefExpectations(
                eatFood, eatPoison, speedAngular, speedLinear
        ));



        nqSpine.goal.put(new BeliefReward(n, "eat:food"), new MutableFloat(1f));
        nqSpine.goal.put(new NotBeliefReward(n, "eat:poison"), new MutableFloat(0.9f));
        //nq.reward.put(new BeliefReward(n, "speed:forward"), new MutableFloat(0.1f));
        

        nqSpine.output.addAll(
                Stream.of(n.terms(
                        motorStop,
                        fire,
                        motorForward, motorBackward, motorLeft, motorRight))
                .map(t -> new InputTask(n, t, Symbols.GOAL, false))
                .collect(Collectors.toList())
        );


//        r.addEye(nq, r.torso, 8, new Vec2(3f, 0f), 0.2f, 0f, 2.5f);
//
//        r.addEye(nq, r.torso, 8, new Vec2(-1f, -2f), 0.2f, -1.7f, 2.5f);


        float pi = (float) Math.PI;

        //nearsight
        r.addEyeWithMouth("n", nqSpine, r.torso, 11, 1, new Vec2(2.7f,0), 0.4f, 0, 20f, pi / 6f);

        //farsight report http://farsight.org/
        r.addEye("f", nqSpine, r.torso, 3, 4, new Vec2(2.7f,0), 0.6f, 0, 35f,(e) -> {});


        //arms have their own controller but the two main inputs are controlled by the master Q 'nqSpine'

//        Arm al = r.addArm("al", nqArm /* ... */, -1.75f, 1.5f, 0.8f); //pi * 1.5f
//        Arm ar = r.addArm("ar", nqArm /* ... */, -1.75f, -1.5f, -0.8f);
//        nqSpine.outs.addAll(al.controls);
//        nqSpine.outs.addAll(ar.controls);



        Arm ac = r.addArm("ac", new NarQ(n)/* ... */, 0, 0, 0); //pi * 1.5f
        nqSpine.output.addAll(ac.controls);
        Arm ad = r.addArm("ad", new NarQ(n)/* ... */, 0, 0, 0); //pi * 1.5f
        nqSpine.output.addAll(ad.controls);

//        Arm ad = r.addArm("ad", nqArm /* ... */, 0, 0, 0); //pi * 1.5f
//        nqSpine.output.addAll(ad.controls);
//        Arm ae = r.addArm("ae", nqArm /* ... */, 0, 0, 0); //pi * 1.5f
//        nqSpine.output.addAll(ae.controls);


    }
	// private static class InputActivationController extends CycleReaction {
	//
	// private final NAR nar;
	//
	// final int windowSize;
	//
	// final DescriptiveStatistics busyness;
	//
	// public InputActivationController(NAR nar) {
	// super(nar);
	// this.nar = nar;
	// this.windowSize = nar.memory.duration();
	// this.busyness = new DescriptiveStatistics(windowSize);
	// }
	//
	// @Override
	// public void onCycle() {
	//
	// final float bInst = nar.memory.emotion.busy();
	// busyness.addValue(bInst);
	//
	// // float bAvg = (float)busyness.getMean();
	//
	// // float busyMax = 3f;
	//
	// // double a = nar.memory.inputActivationFactor.get();
	// // if (bAvg > busyMax) {
	// // a -= 0.01f;
	// // }
	// // else {
	// // a += 0.01f;
	// // }
	// //
	// // final float min = 0.01f;
	// // if (a < min) a = min;
	// // if (a > 1f) a = 1f;
	// //
	// // //System.out.println("act: " + a + " (" + bInst + "," + bAvg);
	// //
	// // nar.param.inputActivationFactor.set(a);
	// // nar.param.conceptActivationFactor.set( 0.5f * (1f + a) /** half as
	// attenuated */ );
	// }
	// }
}
