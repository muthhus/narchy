package nars.rover.run;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.guifx.NARMenu;
import nars.guifx.NARfx;
import nars.guifx.NARtop;
import nars.guifx.chart.MatrixImage;
import nars.guifx.chart.Plot2D;
import nars.guifx.chart.PlotBox;
import nars.guifx.nars.LoopPane;
import nars.guifx.nars.NARPlot;
import nars.guifx.util.ColorArray;
import nars.nar.AbstractNAR;
import nars.nar.Default;
import nars.rover.Sim;
import nars.rover.robot.Arm;
import nars.rover.robot.NARover;
import nars.rover.world.FoodSpawnWorld1;
import nars.term.TermIndex;
import nars.op.NarQ;
import nars.op.NarQ.BeliefReward;
import nars.op.NarQ.InputTask;
import nars.op.NarQ.NotBeliefReward;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.signal.SensorConcept;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nars.guifx.NARfx.newWindow;
import static nars.guifx.NARfx.scrolled;
import static nars.guifx.chart.Plot2D.Line;
import static nars.rover.robot.NARover.*;

/**
 * Created by me on 6/20/15.
 */
public class SomeRovers {

    public static final String motorLeft = "motor(left)";
    public static final String motorRight = "motor(right)";
    public static final String motorForward = "motor(fore)";
    public static final String motorBackward = "motor(back)";
    public static final String motorStop = "motor(stop)";
    public static final String fire = "turret(fire)";



    //public static final SimulatedClock clock = new SimulatedClock();

    public static void main(String[] args) {

        Global.DEBUG = false;
        Global.EXIT_ON_EXCEPTION = false;

        //RoverWorld world = new GridSpaceWorld(GridSpaceWorld.newMazePlanet());
        final Sim game = new Sim(new World());

        //RoverWorld world = new ReactorWorld(32, 48, 32);
        new FoodSpawnWorld1(game, 128, 48, 48, 0.5f);




        /*game.add(new Spider("spider",
                3, 3, 0.618f, 30, 30));*/

        boolean addNARRover = true;
        boolean addQRover = true;
        boolean gui = true;

        if (addNARRover) {
            Default n;
            game.add(new NARover("r1", n = newNAR()) {
                @Override
                public void init(Sim p) {
                    super.init(p);

                    List<SensorConcept>[] sensors = q(this);

                    if (gui) {
                        NARfx.run(() -> {

                            newWindow("NAR",
                                new VBox(
                                    new NARMenu(n),
                                    new NARPlot(n,
                                        new Plot2D(Line, 64, 256, 256)
                                            .add("busy", ()->n.emotion.busy.getSum())
                                            .add("stress", ()->n.emotion.stress.getSum())
                                            .add("frustration", ()->n.emotion.frustration.getSum())
                                    )
                                    //new LoopPane(n)
                                ),
                            400,200);

                            newWindow("Motors",
                                new VBox(
                                    updateMatrices(sensors, n),
                                    new HBox(
                                            scrolled(new NARtop(n).addAll(
                                                        "MotorControls(#x,motor,(),#z)",
                                                        motorLeft, motorRight,
                                                        motorForward, motorBackward,
                                                        motorStop /*, turretFire */)),
                                            scrolled(new NARtop(n).addAll(
                                                        EAT_FOOD.toString(),
                                                        EAT_POISON.toString(),
                                                        SPEED_LEFT.toString(),
                                                        SPEED_RIGHT.toString(),
                                                        SPEED_FORE.toString(),
                                                        SPEED_BACK.toString()

                                            ))
                                    )
                                )
                            );
                        });
                    }
                }

            });

        }

//        if (addQRover) {
//            game.add(new QRover("r2"));
//        }

//        {
//            NAR nar = new Default();
//
//            //nar.paranar.outputVolume.set(0);
//
//            game.add(new CarefulRover("r2", nar));
//        }
        float fps = 30;
        game.run(fps);

    }

    private static Node updateMatrices(List<SensorConcept>[] sensors, Default n) {
        VBox v = new VBox();
        for (List<SensorConcept> l : sensors) {
            v.getChildren().add(updateMatrix(l, n));
        }
        return v;
    }

    private static MatrixImage updateMatrix(List<SensorConcept> sensor, NAR n) {
        MatrixImage mp = new MatrixImage();

        long now = n.time();
        int dur = n.duration();
        n.onFrame(nn->{
            mp.set(sensor.size(), 2,
                (x,y) -> {
                    SensorConcept s = sensor.get(x);
                    //float exp = s.beliefs().truth(now,dur).expectation();
                    //float value = s.get();

                    switch (y) {
                        case 0:
                            float b = s.beliefs().truth(now, dur).expectation();
                            return ColorArray.rgba(b, 1-b, 0, 1f);
                        case 1:
                            float g = s.goals().truth(now, dur).expectation();
                            return ColorArray.rgba(0, g, 1-g, 1f);
                    }

                    return 0;
                }
            );
        });
        mp.setFitHeight(60);
        mp.setFitWidth(sensor.size()*20);

        return mp;
    }

    public static Default newNAR() {
        int conceptsFirePerCycle = 32;

        Random rng = new XorShift128PlusRandom(1);
        TermIndex index = new AbstractNAR.WeakTermIndex(32 * 1024, rng);
        Default nar = new Default(
                //new Memory(clock, TermIndex.softMemory(64*1024)),
                1200, conceptsFirePerCycle, 2, 3, rng, index);
        /*nar.with(
                Anticipate.class,
                Inperience.class
        );*/

        /*PrologCore p = new PrologCore(nar);
        p.confThreshold.setValue(0.7f);*/

//        nar.input("$0.8$ ((?y <-> ^MotorControls) && ( ?y ==> ?x ))?");
//        nar.input("$0.8$ <?x ==> [food]>?");
//        nar.input("$0.8$ <food <-> poison>?");
//        nar.input("$0.8$ <[food] <-> [poison]>?");

        nar.logSummaryGT(System.out, 0.7f);
//        nar.log(Systenar.out, x -> {
//            if (x instanceof Task) {
//                Task t = (Task)x;
//                //if (t.isInput()) return false;
//                return t.budget().summary() > 0.0f;
//            }
//            return false;
//        });


        nar.DEFAULT_JUDGMENT_PRIORITY = 0.5f;
//            nar.memory.DEFAULT_JUDGMENT_DURABILITY = 0.35f;
        nar.DEFAULT_GOAL_PRIORITY = 0.5f;
//            nar.memory.DEFAULT_GOAL_DURABILITY = 0.7f;
        nar.DEFAULT_QUESTION_PRIORITY = 0.4f;

//            nar.memory.DEFAULT_QUESTION_DURABILITY = 0.6f;
        //nar.initNAL9();

        //nar.memory.perfection.setValue(0.15f);
        //nar.core.confidenceDerivationMin.setValue(0.01f);


        //nar.core.activationRate.setValue(1f / conceptsFirePerCycle /* approxmimate */);
        nar.core.activationRate.setValue(0.2f);


        nar.duration.set(2);
        nar.conceptForgetDurations.setValue(2f);
        nar.termLinkForgetDurations.setValue(4);
        nar.taskLinkForgetDurations.setValue(3);

        nar.cyclesPerFrame.set(3);
        nar.shortTermMemoryHistory.set(3);

        nar.executionThreshold.setValue(0.01f);
        nar.derivationDurabilityThreshold.setValue(0.01f);

        return nar;
    }


    /**
     * attaches a prosthetic q-controller to a NAR
     */
    public static List<SensorConcept>[] q(NARover r) {
        NAR n = r.nar;


        NarQ nqSpine = new NarQ(n, (i, o) -> (int) Math.ceil(1 + Math.sqrt(i * o)));


        nqSpine.power.setValue(0.05f);

        nqSpine.input.addAll(nqSpine.getBeliefMotivations(SPEED_LEFT, SPEED_RIGHT, SPEED_FORE, SPEED_BACK, EAT_FOOD, EAT_POISON));


        nqSpine.goal.put(new BeliefReward(n, EAT_FOOD), new MutableFloat(1f));
        nqSpine.goal.put(new NotBeliefReward(n, EAT_POISON), new MutableFloat(0.9f));
        //nq.reward.put(new BeliefReward(n, "speed:forward"), new MutableFloat(0.1f));


        nqSpine.output.addAll(
                Stream.of(n.terms(
                        motorStop,
                        //fire,
                        motorForward, motorBackward, motorLeft, motorRight))
                        .map(t -> new InputTask(n, t, Symbols.GOAL, false))
                        .collect(Collectors.toList())
        );


//        r.addEye(nq, r.torso, 8, new Vec2(3f, 0f), 0.2f, 0f, 2.5f);
//
//        r.addEye(nq, r.torso, 8, new Vec2(-1f, -2f), 0.2f, -1.7f, 2.5f);


        float pi = (float) Math.PI;

        //nearsight
        float dist = 20f;

        Vec2 front = new Vec2(2.7f, 0);

        //feeler
        List<SensorConcept> whisker = r.addEyeWithMouth(r, "t", nqSpine, r.torso, 3, 4, front,
                0.5f, 0, dist / 6f, 0.2f);


        //nearsight & mouth
        List<SensorConcept> nearSight = r.addEyeWithMouth(r, "n", nqSpine, r.torso, 7, 2, front,
                0.5f, 0, dist / 2f, 0.2f);


        //farsight
        List<SensorConcept> farSight = r.addEye(r, "f", nqSpine, r.torso, 5, 5, front,
                1.25f, 0, dist, (e) -> {
                });

        //reverse
        List<SensorConcept> backSight = r.addEye(r, "b", nqSpine, r.torso, 3, 9, new Vec2(-0.5f, 0),
                1.25f, pi / 2f, dist / 2f, (e) -> {
                });


        //arms have their own controller but the two main inputs are controlled by the master Q 'nqSpine'

//        Arm al = r.addArm("al", nqArm /* ... */, -1.75f, 1.5f, 0.8f); //pi * 1.5f
//        Arm ar = r.addArm("ar", nqArm /* ... */, -1.75f, -1.5f, -0.8f);
//        nqSpine.outs.addAll(al.controls);
//        nqSpine.outs.addAll(ar.controls);


        Arm ac = r.addArm(r, "ac", new NarQ(n)/* ... */, 0, 0, 0); //pi * 1.5f
        nqSpine.output.addAll(ac.controls);
        Arm ad = r.addArm(r, "ad", new NarQ(n)/* ... */, 0, 0, 0); //pi * 1.5f
        nqSpine.output.addAll(ad.controls);

//        Arm ad = r.addArm("ad", nqArm /* ... */, 0, 0, 0); //pi * 1.5f
//        nqSpine.output.addAll(ad.controls);
//        Arm ae = r.addArm("ae", nqArm /* ... */, 0, 0, 0); //pi * 1.5f
//        nqSpine.output.addAll(ae.controls);

        return new List[] {
                farSight, nearSight, whisker, backSight
        };


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
    // // //Systenar.out.println("act: " + a + " (" + bInst + "," + bAvg);
    // //
    // // nar.paranar.inputActivationFactor.set(a);
    // // nar.paranar.conceptActivationFactor.set( 0.5f * (1f + a) /** half as
    // attenuated */ );
    // }
    // }
}
