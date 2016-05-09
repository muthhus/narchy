package nars.rover.run;

import com.artemis.Entity;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.Op;
import nars.guifx.NARfx;
import nars.guifx.NARtop;
import nars.guifx.chart.MatrixImage;
import nars.guifx.util.ColorArray;
import nars.nal.Tense;
import nars.nar.AbstractNAR;
import nars.nar.Default;
import nars.rover.Sim;
import nars.rover.obj.*;
import nars.rover.run.DemoRovers.ManualControl.ManualOverride;
import nars.rover.world.FoodSpawnWorld1;
import nars.task.Task;
import nars.term.TermIndex;
import nars.time.FrameClock;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.signal.SensorConcept;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World2D;

import java.util.EnumMap;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

import static javafx.application.Platform.runLater;
import static nars.guifx.NARfx.newWindow;
import static nars.guifx.NARfx.scrolled;
import static nars.rover.obj.NARover.*;

/**
 * Created by me on 6/20/15.
 */
public class DemoRovers {

    public static final String motorLeft = "motor(left)";
    public static final String motorRight = "motor(right)";
    public static final String motorFore = "motor(fore)";
    public static final String motorBack = "motor(back)";
    public static final String motorStop = "motor(stop)";
    public static final String turretFire = "turret(fire)";


    public static class ManualControl<R> extends HBox {
        private final R r;
        private final ToggleButton manualToggle;
        private final Button freezeButton;
        private final ToggleButton trainToggle;
        private final ManualOverride<R>[] actions;

        public static class ManualOverride<R> {

            private final KeyCode key;
            private final Consumer<R> onPress, onRelease;
            private final Function<R, Node> guiBuilder;

            public ManualOverride(KeyCode key, Consumer<R> onPress, Consumer<R> onRelease, Function<R, Node> guiMaker) {
                this.key = key;
                this.onPress = onPress;
                this.onRelease = onRelease;
                this.guiBuilder = guiMaker;
            }

            public void freeze() {

            }
        }

        final EnumMap<KeyCode, ManualOverride> keyActions = new EnumMap(KeyCode.class);

        public ManualControl(R r, ManualOverride<R>... mm) {
            super();

            this.r = r;
            this.actions = mm;

            getChildren().add(new VBox(
                    manualToggle = new ToggleButton("MANUAL"),
                    freezeButton = new Button("FREEZE"),
                    trainToggle = new ToggleButton("TRAIN..")
            ));

            //manualToggle.selectedProperty().bind()

            freezeButton.setOnAction(e -> {
                for (ManualOverride<R> m : actions) {
                    m.freeze();
                }
            });

            for (ManualOverride<R> m : mm) {
                keyActions.put(m.key, m);
                Node n = m.guiBuilder.apply(r);
                getChildren().add(n);
            }

            runLater(() -> {

//                setOnKeyTyped(e->{
//                    System.out.println("typed: " + e);
//                });
//                setOnKeyPressed(pressed -> {
//                    //if (!isManualable())
//                        //return;
//                    System.out.println("pressed: " + pressed);
//                    ManualOverride ma = keyActions.get(pressed.getCode());
//                    if (ma != null)
//                        ma.onPress.accept(r);
//                });
                getScene().setOnKeyPressed(e -> {
                    System.out.println("pressd: " + e);
                    //if (!isManualable())
                    //return;
                    ManualOverride ma = keyActions.get(e.getCode());
                    if (ma != null)
                        ma.onPress.accept(r);
                });
            });

        }

        public boolean isManualable() {
            return manualToggle.isPressed();
        }
    }


    public static void main(String[] args) {

        Global.DEBUG = false;
        Global.EXIT_ON_EXCEPTION = false;

        final Sim sim = new Sim(new World2D());



        //RoverWorld world = new GridSpaceWorld(GridSpaceWorld.newMazePlanet());
        //RoverWorld world = new ReactorWorld(32, 48, 32);
        new FoodSpawnWorld1(sim, 128, 48, 48, 0.5f);

        {
            Grid2D g = Grid2D.newMazePlanet(50, 30, 3);
            sim.game.createEntity().edit()
                    .add(g)
                    .add(new DrawAbove(g)); //HACK
        }


        //.add(new RespawnOnDeath())
        //.add(new Gravity())
        //.add(new WallSensor())
        //.add(new PlayerControlled())
        //.add(new Bounds(G.CELL_SIZE, G.CELL_SIZE)).getEntity();







        /*game.add(new Spider("spider",
                3, 3, 0.618f, 30, 30));*/

        boolean gui = true;

        Default nar = newNAR();

        Entity e = sim.game.createEntity().edit()
                .add(new Physical(
                        AbstractPolygonBot.newDynamic(0, 0),
                        AbstractPolygonBot.newTriangle()))
                .add(new Health(10))
                .add(new Motorized())
                .add(new RunNAR(nar))
                .getEntity();

        sim.game.process(); //initialize


        NARover nr = new NARover("r1", e,
            nar);

        nar.goal(EAT_FOOD, Tense.Eternal, 1f, 0.9f);//"eat:food! %1.00|0.95%");
        nar.goal(EAT_POISON, Tense.Eternal, 0f, 0.9f);

        List<SensorConcept>[] sensors = q(nr);

        if (gui) {
            NARfx.run(() -> {

//                            newWindow("NAR",
//                                new VBox(
//                                    new NARMenu(n),
//                                    new NARPlot(n,
//                                        new Plot2D(Line, 64, 256, 256)
//                                            .add("busy", ()->n.emotion.busy.getSum())
//                                            .add("stress", ()->n.emotion.stress.getSum())
//                                            .add("frustration", ()->n.emotion.frustration.getSum())
//                                    )
//                                    //new LoopPane(n)
//                                ),
//                            400,200);

                float inputStrengthf = 0.9f;
                Stage stage = newWindow("Motors",
                        new VBox(
                                new ManualControl<AbstractRover>(nr,
                                        new ManualOverride<AbstractRover>(KeyCode.NUMPAD5,
                                                (r) -> {
                                                    System.out.println("forward");

                                                    for (String c : new String[] { motorFore, motorBack, motorLeft, motorRight, }) {
                                                        nar.believe(c, Tense.Present, 0f, inputStrengthf);
                                                        nar.goal(c, Tense.Present, 0.0f, inputStrengthf);
                                                    }
                                                },
                                                (r) -> {
                                                    //System.out.println("up -");
                                                },
                                                (r) -> {
                                                    return new Button("FWD");
                                                }
                                        ),
                                        new ManualOverride<AbstractRover>(KeyCode.NUMPAD8,
                                                (r) -> {
                                                    System.out.println("forward");
                                                    nar.believe(motorFore, Tense.Present, 0f, inputStrengthf);
                                                    nar.goal((motorFore), Tense.Present, 1f, inputStrengthf);
                                                    nar.believe(motorBack, Tense.Present, 0f, inputStrengthf);
                                                    nar.goal((motorBack), Tense.Present, 0f, inputStrengthf);
                                                },
                                                (r) -> {
                                                    //System.out.println("up -");
                                                },
                                                (r) -> {
                                                    return new Button("FWD");
                                                }
                                        ),
                                        new ManualOverride<AbstractRover>(KeyCode.NUMPAD2,
                                                (r) -> {
                                                    System.out.println("back");
                                                    nar.believe(motorBack, Tense.Present, 0f, inputStrengthf);
                                                    nar.goal((motorBack), Tense.Present, 1f, inputStrengthf);
                                                    nar.believe(motorFore, Tense.Present, 0f, inputStrengthf);
                                                    nar.goal((motorFore), Tense.Present, 0f, inputStrengthf);
                                                },
                                                (r) -> {
                                                },
                                                (r) -> {
                                                    return new Button("BACK");
                                                }
                                        ),
                                        new ManualOverride<AbstractRover>(KeyCode.NUMPAD4,
                                                (r) -> {
                                                    System.out.println("left");
                                                    nar.goal((motorLeft), Tense.Present, 1f, inputStrengthf);
                                                    nar.goal((motorRight), Tense.Present, 0f, inputStrengthf);
                                                    nar.believe(motorLeft, Tense.Present, 0f, inputStrengthf);
                                                    nar.believe(motorRight, Tense.Present, 0f, inputStrengthf);
                                                },
                                                (r) -> {
                                                    //System.out.println("up -");
                                                },
                                                (r) -> {
                                                    return new Button("LEFT");
                                                }
                                        ),
                                        new ManualOverride<AbstractRover>(KeyCode.NUMPAD6,
                                                (r) -> {
                                                    System.out.println("right");
                                                    nar.goal((motorRight), Tense.Present, 1f, inputStrengthf);
                                                    nar.goal((motorLeft), Tense.Present, 0f, inputStrengthf);
                                                    nar.believe(motorLeft, Tense.Present, 0f, inputStrengthf);
                                                    nar.believe(motorRight, Tense.Present, 0f, inputStrengthf);
                                                },
                                                (r) -> {
                                                },
                                                (r) -> {
                                                    return new Button("RIGHT");
                                                }
                                        )
                                ),
                                updateMatrices(sensors, nar),
                                new HBox(
                                        scrolled(new NARtop(nar).addAll(
                                                "motor(#x)",
                                                motorLeft, motorRight,
                                                motorFore, motorBack,
                                                motorStop, turretFire)),
                                        scrolled(new NARtop(nar).addAll(
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
        ;

        float fps = 30;
        sim.run(fps);

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
        n.onFrame(nn -> {
            mp.set(sensor.size(), 2,
                    (x, y) -> {
                        SensorConcept s = sensor.get(x);
                        //float exp = s.beliefs().truth(now,dur).expectation();
                        //float value = s.get();

                        switch (y) {
                            case 0:
                                float b = s.beliefs().truth(now).expectation();
                                return ColorArray.rgba(b, 1 - b, 0, 1f);
                            case 1:
                                float g = s.goals().truth(now).expectation();
                                return ColorArray.rgba(0, g, 1 - g, 1f);
                        }

                        return 0;
                    }
            );
        });
        mp.setFitHeight(60);
        mp.setFitWidth(sensor.size() * 20);

        return mp;
    }

    public static Default newNAR() {
        int conceptsFirePerCycle = 4;

        Random rng = new XorShift128PlusRandom(1);
        Default nar = new Default(
                1200, conceptsFirePerCycle, 2, 3, rng,
                new Default.WeakTermIndex(256 * 1024, rng),
                new FrameClock());
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

        //nar.logSummaryGT(System.out, 0.35f);
//        nar.log(System.out, x -> {
//            if (x instanceof Task) {
//                Task t = (Task)x;
//                //if (t.isInput()) return false;
//                if (t.op().in(Op.ImplicationOrEquivalenceBits))
//                    return t.budget().summary() > 0.0f;
//            }
//            return false;
//        });


        nar.DEFAULT_JUDGMENT_PRIORITY = 0.4f;
//            nar.memory.DEFAULT_JUDGMENT_DURABILITY = 0.35f;
        nar.DEFAULT_GOAL_PRIORITY = 0.7f;
//            nar.memory.DEFAULT_GOAL_DURABILITY = 0.7f;
        nar.DEFAULT_QUESTION_PRIORITY = 0.3f;

//            nar.memory.DEFAULT_QUESTION_DURABILITY = 0.6f;
        //nar.initNAL9();

        //nar.memory.perfection.setValue(0.15f);
        //nar.core.confidenceDerivationMin.setValue(0.01f);


        //nar.core.activationRate.setValue(1f / conceptsFirePerCycle /* approxmimate */);
        nar.conceptActivation.setValue(0.25f);


        nar.conceptRemembering.setValue(3f);
        nar.termLinkRemembering.setValue(12);
        nar.taskLinkRemembering.setValue(8);

        nar.cyclesPerFrame.set(32);
        nar.shortTermMemoryHistory.set(2);

        //nar.executionThreshold.setValue(0.01f);
        //nar.derivationDurabilityThreshold.setValue(0.01f);

        return nar;
    }


    /**
     * attaches a prosthetic q-controller to a NAR
     */
    public static List<SensorConcept>[] q(NARover r) {

        //NarQ nqSpine = new NarQ(n, (i, o) -> (int) Math.ceil(1 + Math.sqrt(i * o)));


        //nqSpine.power.setValue(0.05f);

        //nqSpine.input.addAll(nqSpine.getBeliefMotivations(SPEED_LEFT, SPEED_RIGHT, SPEED_FORE, SPEED_BACK, EAT_FOOD, EAT_POISON));


        //nqSpine.goal.put(new BeliefReward(n, EAT_FOOD), new MutableFloat(1f));
        //nqSpine.goal.put(new NotBeliefReward(n, EAT_POISON), new MutableFloat(0.9f));
        //nq.reward.put(new BeliefReward(n, "speed:forward"), new MutableFloat(0.1f));


        /*
        nqSpine.output.addAll(
                Stream.of(n.terms(
                        motorStop,
                        turretFire,
                        motorForward, motorBackward, motorLeft, motorRight))
                        .map(t -> new InputTask(n, t, Symbols.GOAL, false))
                        .collect(Collectors.toList())
        );
        */


//        r.addEye(nq, r.torso, 8, new Vec2(3f, 0f), 0.2f, 0f, 2.5f);
//
//        r.addEye(nq, r.torso, 8, new Vec2(-1f, -2f), 0.2f, -1.7f, 2.5f);


        float pi = (float) Math.PI;

        //nearsight
        float dist = 20f;

        Vec2 front = new Vec2(2.7f, 0);

        Body torso = r.entity.getComponent(Physical.class).body;
        assert(torso!=null);

        //feeler
        List<SensorConcept> whisker = r.addEyeWithMouth("t", torso, 3, 4, front,
                0.5f, 0, dist / 6f, 0.2f);


        //nearsight & mouth
        List<SensorConcept> nearSight = r.addEyeWithMouth("n", torso, 3, 2, front,
                0.5f, 0, dist / 2f, 0.2f);


        //farsight
        List<SensorConcept> farSight = r.addEye("f", torso, 3, 5, front,
                1.25f, 0, dist, (e) -> {
                });

        //reverse
        List<SensorConcept> backSight = r.addEye("b", torso, 3, 6, new Vec2(-0.5f, 0),
                1.25f, pi / 2f, dist / 2f, (e) -> {
                });


        //arms have their own controller but the two main inputs are controlled by the master Q 'nqSpine'

//        Arm al = r.addArm("al", nqArm /* ... */, -1.75f, 1.5f, 0.8f); //pi * 1.5f
//        Arm ar = r.addArm("ar", nqArm /* ... */, -1.75f, -1.5f, -0.8f);
//        nqSpine.outs.addAll(al.controls);
//        nqSpine.outs.addAll(ar.controls);


        //Arm ac = r.addArm(r, "ac", new NarQ(n)/* ... */, 0, 0, 0); //pi * 1.5f
        //nqSpine.output.addAll(ac.controls);
        //Arm ad = r.addArm(r, "ad", new NarQ(n)/* ... */, 0, 0, 0); //pi * 1.5f
        //nqSpine.output.addAll(ad.controls);


        return new List[]{
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
