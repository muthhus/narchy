/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.rover.robot;

import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.op.java.MethodOperator;
import nars.op.java.Lobjects;
import nars.rover.Material;
import nars.rover.Sim;
import nars.rover.obj.NARVisionRay;
import nars.rover.obj.VisionRay;
import nars.rover.run.SomeRovers;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.util.data.Util;
import nars.op.NarQ;
import nars.util.learn.Sensor;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

import static nars.util.Texts.n2;


/**
 * Triangular mobile vehicle
 */
public class NARover extends AbstractPolygonBot {

    public final NAR nar;

    final Lobjects objs;

    float hungry, sick;
    final Sensor linearSpeedFwd, leftSpeed,
            //rightSpeed,
            hungrySensor, sickSensor;


//    final SimpleAutoRangeTruthFrequency linearVelocity;
//    final SimpleAutoRangeTruthFrequency motionAngle;
//    final SimpleAutoRangeTruthFrequency facingAngle;

    //public class DistanceInput extends ChangedTextInput

    final static Logger logger = LoggerFactory.getLogger(NARover.class);

    private MotorControls motors;
    private Turret gun;

    public NARover(String id, NAR nar) {
        super(id);

        this.nar = nar;

        objs = new Lobjects(nar);

        int maxUpdateTime = 32;


        hungry = 1f;
        sick = 0f;

        FloatToFloatFunction speedThresholdToFreq = (speed) -> {
            return speed < 0.01 ? 0 : Util.clamp(0.5f + speed);
        };
        FloatToFloatFunction sigmoid = (speed) -> {
            return Util.sigmoid(speed);
        };

        Term speedForward = nar.term("speed:forward");
        //Term speedBackward = nar.term("speed:backward");
        Vec2 forwardVec = new Vec2(1,0f);
        Vec2 tmp = new Vec2();
        FloatFunction<Term> linearSpeed = (t) -> {
            torso.getWorldPointToOut(forwardVec, tmp);
            return Vec2.dot(torso.getLinearVelocity(), tmp) / 2f /* sensitivity */;
            //float v = tmp.length()/ linearThrustPerCycle / 1.25f;
            //if (v >= 0 && t == speedForward) return v;
            //else if (v <= 0 && t == speedBackward) return -v;
            //return 0;
        };
        this.linearSpeedFwd = new Sensor(nar, speedForward,
                linearSpeed, sigmoid).maxTimeBetweenUpdates(maxUpdateTime);
        /*this.linearSpeedBack = new Sensor(nar, speedBackward,
                linearSpeed, speedThresholdToFreq).maxTimeBetweenUpdates(maxUpdateTime);*/

        Term speedLeft = nar.term("speed:angular");
        FloatFunction<Term> angleSpeed = (t) -> {
            float a = torso.getAngularVelocity();
//            if (a <= 0 && t == speedLeft) return -a;
//            else if (a >= 0 && t == speedRight) return a;
            //return 0;
            return a;
        };
        leftSpeed = new Sensor(nar, speedLeft, angleSpeed, sigmoid).maxTimeBetweenUpdates(maxUpdateTime);


        //TODO torso angle


        hungrySensor = new Sensor(nar, nar.term("eat:food"), (t) -> {
            return 1f-hungry;
        }, speedThresholdToFreq);
        sickSensor = new Sensor(nar, nar.term("eat:poison"), (t) -> {
            return sick;
        }, speedThresholdToFreq);

//
//        linearVelocity = new SimpleAutoRangeTruthFrequency(nar, nar.term("<motion-->[linear]>"), new AutoRangeTruthFrequency(0.0f));
//        motionAngle = new SimpleAutoRangeTruthFrequency(nar, nar.term("<motion-->[angle]>"), new BipolarAutoRangeTruthFrequency());
//        facingAngle = new SimpleAutoRangeTruthFrequency(nar, nar.term("<motion-->[facing]>"), new BipolarAutoRangeTruthFrequency());



    }

    @Override
    protected void onEat(Body eaten, Material m) {
        if (m instanceof Sim.FoodMaterial) {
            logger.warn("food");
            //nar.input("eat:food. :|: %1.0;0.9%");
            hungry = Util.clamp(hungry - 0.85f);

            //nar.input("goal:{food}. :|: %1.00;0.75%");
            //nar.input("goal:{health}. :|: %1.00;0.75%");
        }
        else if (m instanceof Sim.PoisonMaterial) {
            logger.warn("poison");
            //nar.input("eat:poison. :|:");
            sick = Util.clamp(sick + 0.85f);

            //nar.input("goal:{food}. :|: %0.00;0.90%");
            //nar.input("goal:{health}. :|: %0.00;0.90%");
        }

    }

    @Override
    public void step(int time) {
        super.step(time);

        gun.step(time);

        try {
            nar.step();
        } catch (RuntimeException e) {
            e.printStackTrace();
            nar.stop();
        }

        sick *= 0.97f;
        hungry = Util.clamp(hungry + 0.05f);
    }

    public void inputMission() {

        //alpha curiosity parameter
        //long t = nar.time();


        //nar.input("<{left,right,forward,reverse} --> direction>.");
        //nar.input("<{wall,empty,food,poison} --> material>.");
        //nar.input("<{0,x,xx,xxx,xxxx,xxxxx,xxxxxx,xxxxxxx,xxxxxxxx,xxxxxxxxx,xxxxxxxxxx} --> magnitude>.");
        //nar.input("<{0,1,2,3,4,5,6,7,8,9} --> magnitude>.");

        //nar.input("< ( ($n,#x) &| ($n,#y) ) =/> lessThan(#x,#y) >?");

        /*
        for (int i = 0; i < 2; i++) {
            String x = "lessThan(" + XORShiftRandom.global.nextInt(10) + "," +
                    XORShiftRandom.global.nextInt(10) + ")?";

            nar.input(x);
        }
        */

//        nar.input("<0 <-> x>. %0.60;0.60%");
//        nar.input("<x <-> xx>. %0.60;0.60%");
//        nar.input("<xx <-> xxx>. %0.60;0.60%");
//        nar.input("<xxx <-> xxxx>. %0.60;0.60%");
//        nar.input("<xxxx <-> xxxxx>. %0.60;0.60%");
//        nar.input("<xxxxx <-> xxxxxx>. %0.60;0.60%");
//        nar.input("<xxxxxx <-> xxxxxxx>. %0.60;0.60%");
//        nar.input("<xxxxxxx <-> xxxxxxxxx>. %0.60;0.60%");
//        nar.input("<xxxxxxxx <-> xxxxxxxxxx>. %0.60;0.60%");
//        nar.input("<0 <-> xxxxxxxxx>. %0.00;0.90%");

        //nar.input("goal:{health}! %1.0;0.95%");
        //nar.input("goal:{health}! :|:");

        //nar.believe("goal:{health}", Tense.Present, 0.5f, 0.9f); //reset



        //nar.input("<?x ==> goal:{$y}>? :|:");

        try {

            /*if (t < 1500)
                train(t);
            else */ if (mission == 0) {
                //seek food
                //curiosity = 0.05f;

                //nar.goal("goal:{food}", 1.00f, 0.90f);
                //nar.input("goal:{food}!");

                //clear appetite:
                //nar.input("eat:food. :|: %0.00;0.75%");
                //nar.input("eat:poison. :|: %0.00;0.75%");

                nar.input("eat:food! %1.00|0.95%");

                nar.input("speed:forward! %1.00;0.7%");
                nar.input("eat:poison! %0.0|0.9%");
                //nar.input("(--, <eat:food <-> eat:poison>). %1.00;0.95%");
                //nar.input("(?x ==> eat:#y)?");
                //nar.input("(?x && eat:#y)?");

                //nar.input("MotorControls(#x,motor,(),#z)! :|: %1.0;0.25%"); //create demand for action
                //nar.input("MotorControls(?x,motor,(),#z)! :|: %1.0;0.25%");
                //nar.concept("MotorControls(?x,motor,?y,#z)").print();
                //nar.concept("MotorControls(#x,motor,#y,#z)").print();


                //((Default)nar).core.active.printAll();
                //nar.concept("MotorControls(forward,motor,(),#1)").print();

                //nar.input("motion:#anything! :|:"); //move

            } else if (mission == 1) {
                //rest
                nar.input("eat:#anything! :|: %0%");
                //nar.input("motion:(0, #x)! :|:"); //stop
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //..
    }


    protected void train(long t) {
        //float freq = 0.5f + 0.5f * (1/(1f + t/5000f)), conf = 0.85f;
        float freq = 1f, conf = 0.9f;
        //nar.input("MotorControls(random,motor,(),#x)! %1.0;0.1%");
        nar.input("MotorControls(random,motor,(),#x)! %" + n2(freq) + "|" + n2(conf) + "%");
        System.out.println("@" + t + " Curiosity Trained @ freq=" + freq);
    }

    @Override
    public void init(Sim p) {
        super.init(p);

        try {
            addMotorController();
        } catch (Exception e) {
            e.printStackTrace();
        }

        gun = new Turret(sim);
    }

    @Override
    public BeingMaterial getMaterial() {
        return new NARRoverMaterial(this, nar);
    }

    @Override
    protected Body newTorso() {

        Body torso = newTriangle(getWorld(), mass);

        torso.setUserData(getMaterial());

        return torso;
    }


    /** http://stackoverflow.com/questions/20904171/how-to-create-snake-like-body-in-box2d-and-cocos2dx */
    public Arm addArm(String id, NarQ controller, float ax, float ay, float angle) {

        int segs = 4;
        float segLength = 3.7f;
        float thick = 1.5f;

        Arm a = new Arm(id, sim, torso, ax, ay, angle, segs, segLength, thick);

        addEye(id + "e", controller, a.segments.getLast(), 9, new Vec2(0.5f, 0), 1f, (float)(+Math.PI/4f), 2.5f);

        a.joints.forEach((Consumer<RevoluteJoint>) jj -> {

            for (float speed : new float[] { +1 , -1 }){
                controller.output.add(new NarQ.Action() {

                    @Override
                    public void run(float strength) {
                        ///System.out.println("motor " + finalIdx + " " + strength);
                        jj.setMotorSpeed(strength * speed);
                    }

                    @Override
                    public float ran() {
                        return jj.getMotorSpeed();
                    }
                });
            }

        });

        return a;
    }

//    public void addEye() {
//        //List<VisionRay> vision = Global.newArrayList();
//        for (int i = 0; i < retinaPixels; i++) {
//            float aStep = (float) (Math.PI * 2f) / retinaPixels;
//            final float angle = aStep * i;
//
//            VisionRay v = new NARVisionRay(nar, torso,
//                    /*eats ?*/ mouthPoint /*: new Vec2(0,0)*/,
//                    angle, aStep, retinaRaysPerPixel, L, 1f/retinaPixels) {
//            };
//
//
//            //vision.add(v);
//
//            draw.addLayer(v);
//            senses.add(v);
//        }
//
//    }




    public void addEye(String id, NarQ controller, Body base, int detail, Vec2 center, float arc, float centerAngle, float distance) {
        addEye(id, controller, base, 1, detail, center, arc, centerAngle, distance, (v) -> {});
    }
    public void addEyeWithMouth(String id, NarQ controller, Body base, int pixels, int detail, Vec2 center, float arc, float centerAngle, float distance, float mouthArc) {
        addEye(id, controller, base, pixels, detail, center, arc, centerAngle, distance, (v) -> {
            float angle = v.angle;
            v.setEats(((angle < mouthArc / 2f) || (angle > (Math.PI * 2f) - mouthArc / 2f)));
        });
    }

    public void addEye(String id, NarQ controller, Body base, int pixels, int detail, Vec2 center, float arc, float centerAngle, float distance, Consumer<VisionRay> each) {

        float aStep = (float) (Math.PI * 2f)/pixels * (arc);

        //final MutableFloat servo = new MutableFloat();

        for (int i = 0; i < pixels; i++) {
            final float angle = aStep * (i-pixels/2) + centerAngle;

            NARVisionRay v = new NARVisionRay(id + i, nar, base, center, angle, aStep,
                    detail, distance, 1f/pixels) {

//                  @Override public float getLocalAngle () {
//                      return 0.5f * (float)Math.sin(servo.floatValue()); //controller.get();
//                  }


                @Override
                @Deprecated protected void updateColor(Color3f rayColor) {
//                    float s = v.hitDist;
//                    rayColor.set(s,1f-s,0.5f);
                }
            };


            each.accept(v);

            for (String material : new String[]{"food", "poison"}) {

                DoubleSupplier value = () -> {
                    if (v.hit(material)) {
                        return 1f - v.hitDist; //closer = larger number (up to 1.0)
                    }
                    return 0; //nothing seen within the range
                };

                Sensor visionSensor = new Sensor(nar, $.prop(v.visionTerm, $.the(material)),
                        (t) -> (float) value.getAsDouble(), (x) ->
                        x == 0 ? 0 :
                            (0.5f + 0.5f * x)
                );
                visionSensor.setFreqResolution(0.1f);

                controller.input.add(value);
            }

            //draw.addLayer(v);
            senses.add(v);
        }



//        for (float servoSpeed : new float[] { -0.5f, 0.5f } ) {
//            controller.outs.add(new NarQ.Action() {
//
//                public float accum = 0;
//
//                @Override
//                public void run(float strength) {
//                    servo.add(strength * servoSpeed);
//
//                    this.accum = Util.clamp(this.accum + strength);
//
//                }
//
//                @Override
//                public float ran() {
//                    accum *= 0.9f;
//                    return accum;
//                }
//            });
//        }
    }


    protected void addMotorController() throws Exception {

        motors = objs.the("motor", MotorControls.class, this);


    }

    //public static final ConceptDesire strongestTask = (c ->  c.getGoals().topEternal().getExpectation() );


    @Override
    protected void feelMotion() {

//        if (angVelocity < 0.1) {
//            feltAngularVelocity.set("rotated(" + f(0) + "). :|: %0.95;0.90%");
//            //feltAngularVelocity.set("feltAngularMotion. :|: %0.00;0.90%");
//        } else {
//            String direction;
//            if (xa < 0) {
//                direction = sim.angleTerm(-MathUtils.PI);
//            } else /*if (xa > 0)*/ {
//                direction = sim.angleTerm(+MathUtils.PI);
//            }
//            feltAngularVelocity.set("rotated(" + f(angVelocity) + "," + direction + "). :|:");
//            // //feltAngularVelocity.set("<" + direction + " --> feltAngularMotion>. :|: %" + da + ";0.90%");
//        }


        //linearVelocity.observe(linVelocity);


        //Vec2 currentPosition = torso.getWorldCenter();
        //if (!positions.isEmpty()) {
            //Vec2 movement = currentPosition.sub(positions.poll());
            //double theta = Math.atan2(movement.y, movement.x);
            //motionAngle.observe((float)theta);
        //}
        //positions.addLast(currentPosition.clone());


        //String torsoAngle = sim.angleTerm(torso.getAngle());


        //feltMotion.set("(&&+0, speed:{" + f5(linSpeed) + "},angle:{" + torsoAngle + "},rotation:{" + angDir + "," + f5(angSpeed) + "}). :|:");



//        //radians per frame to angVelocity discretized value
//        float xa = torso.getAngularVelocity();
//        float angleScale = 1.50f;
//        String angDir = xa > 0 ? "r" : "l";
//        float angSpeed = (float) (Math.log(Math.abs(xa * angleScale) + 1f)) / 2f;
//        float maxAngleVelocityFelt = 0.8f;
//        if (angSpeed > maxAngleVelocityFelt) {
//            angSpeed = maxAngleVelocityFelt;
//        }
//        feltMotion.set("speed:right. %" + Texts.n2(angSpeedRight) + "|0.8%");
//        feltMotion.set("speed:left. %" + Texts.n2(angSpeedLeft) + "|0.8%");


        //feltMotion.set("rotation:{\" + angDir + \",\" + f5(angSpeed) + \"}. :|:");


        //facingAngle.observe( angVelocity ); // );
        //nar.inputDirect(nar.task("<facing-->[" +  + "]>. :|:"));
        //System.out.println("  " + motion);


        //feltSpeed.set("feltSpeed. :|: %" + sp + ";0.90%");
        //int positionWindow1 = 16;

        /*if (positions.size() >= positionWindow1) {
            Vec2 prevPosition = positions.removeFirst();
            float dist = prevPosition.sub(currentPosition).length();
            float scale = 1.5f;
            dist /= positionWindow1;
            dist *= scale;
            if (dist > 1.0f) {
                dist = 1.0f;
            }
            feltSpeedAvg.set("<(*,linVelocity," + Rover2.f(dist) + ") --> feel" + positionWindow1 + ">. :\\:");
        }*/

    }

    public static class MotorControls {

        public final NARover rover;
        private final Termed left, right, stop, forward;
        private final Termed backward;
        final boolean proportional = true; //thrust proportional to expectation of desire

        public MotorControls(NARover rover) {
            this.rover = rover;

            forward = rover.nar.term(SomeRovers.motorForward);
            backward = rover.nar.term(SomeRovers.motorBackward);
            left = rover.nar.term(SomeRovers.motorLeft);
            right = rover.nar.term(SomeRovers.motorRight);
            stop = rover.nar.term(SomeRovers.motorStop);
        }

        public Truth stop() {
            Task c = MethodOperator.invokingTask();
            rover.thrustRelative(0);
            rover.rotateRelative(0);
            return c.truth(); //TODO use feedback discounting how much actually was stopped already
        }

        private Truth forward(boolean forward) {
            Task c = MethodOperator.invokingTask();
            float thrust = (proportional && c!=null) ? c.motivation() : 1;
            if (!forward) thrust = -thrust;
            rover.thrustRelative(thrust);
            return c.truth();
        }

        private Truth rotate(boolean right) {
            Task c = MethodOperator.invokingTask();
            float thrust = (proportional && c!=null) ? c.motivation() : 1;
            if (right) thrust = -thrust;
            rover.rotateRelative(thrust);
            return c.truth();
        }

        public Truth left() {  return rotate(false); }
        public Truth right() {  return rotate(true); }
        public Truth forward() {  return forward(true); }
        public Truth backward() {  return forward(false); }


        final static Truth unfired = new DefaultTruth(0.5f, 1f);

        public Truth fire() {

            Task c = MethodOperator.invokingTask();
            if (rover.gun.fire(rover.torso, c.motivation())) {
                return c.truth();
            }
            return unfired;
        }

        @Deprecated public Task random() {
            Task c = MethodOperator.invokingTask();

            Termed term;

            switch ((int)(5 * Math.random())) {
                case 0:
                    term = stop; break;
                case 1:
                    term = forward; break;
                case 2:
                    term = backward; break;
                case 3:
                    term = left; break;
                case 4:
                    term = right; break;
                default:
                    term = null;
            }

            return new MutableTask(term, Symbols.GOAL)
                    .budget(c.budget())
                    .truth( c.truth() )
                    .present(rover.nar)
                    .log("Curiosity");
        }
    }



//    /** bipolar cycle desire, which resolves two polar opposite desires into one */
//    public abstract static class BiCycleDesire  {
//
//        private final CycleDesire positive;
//        private final CycleDesire negative;
//
//        public float positiveDesire, negativeDesire;
//        float threshold = 0f;
//        private final NAR nar;
//
//        public BiCycleDesire(String positiveTerm, String negativeTerm, ConceptDesire desireFunction, NAR n) {
//            this.nar = n;
//            this.positive = new CycleDesire(positiveTerm, desireFunction, n) {
//
//                @Override
//                float onFrame(final float desire) {
//                    positiveDesire = desire;
//                    return Float.NaN;
//                }
//            };
//            //this will be executed directly after positive, so we put the event handler in negative
//            this.negative = new CycleDesire(negativeTerm, desireFunction, n) {
//
//                @Override
//                float onFrame(float negativeDesire) {
//                    BiCycleDesire.this.negativeDesire = negativeDesire;
//
//                    frame(positiveDesire, negativeDesire);
//
//
//                    return Float.NaN;
//                }
//
//            };
//        }
//
//        protected void frame(final float positiveDesire, final float negativeDesire) {
//
//            float net = positiveDesire - negativeDesire;
//            boolean isPos = (net > 0);
//            if (!isPos)
//                net = -net;
//
//            if (net > threshold) {
//                final float feedback = onFrame(net, isPos);
//                if (Float.isFinite(feedback)) {
//                    if (isPos) {
//                        float posFeedback = feedback;
//                        nar.input(this.positive.getFeedback(posFeedback));
//                        nar.input(this.negative.getFeedback(0));
//
////                        //counteract the interference
////                        negFeedback = (negativeDesire - (positiveDesire - feedback));
////                        if (negFeedback < 0) negFeedback = 0;
////                        Task iit = this.negative.getFeedback(negFeedback)
////                                .goal()
////                                .truth(negFeedback, 0.75f) //adjust confidence too
////                                .get();
////
////                        nar.inputDirect(iit);
//
//                    } else {
//                        float negFeedback = feedback;
//                        nar.input(this.negative.getFeedback(negFeedback));
//                        nar.input(this.positive.getFeedback(0));
//
////                        //counteract the interference
////                        posFeedback = (positiveDesire - (negativeDesire - feedback));
////                        if (posFeedback < 0) posFeedback = 0;
////                        Task iit = this.positive.getFeedback(posFeedback)
////                                .goal()
////                                .truth(posFeedback, 0.75f)
////                                .get();
////
////                        nar.inputDirect(iit);
//
//
//                    }
//                }
//
//            }
//
//        }
//
//        abstract float onFrame(float desire, boolean positive);
//
//    }
}

//        new CycleDesire("motor(random)", strongestTask, nar) {
//            @Override float onFrame(float desire) {
//                //variable causes random movement
//                double v = Math.random();
//                if (v > (desire - 0.5f)*2f) {
//                    return Float.NaN;
//                }
//
//                //System.out.println(v + " " + (desire - 0.5f)*2f);
//
//                float strength = 0.65f;
//                float negStrength = 1f - strength;
//                String tPos = "%" + strength + "|" + desire + "%";
//                String tNeg = "%" + negStrength + "|" + desire + "%";
//
//                v = Math.random();
//                if (v < 0.25f) {
//                    nar.input(nar.task("motor(left)! " + tPos));
//                    nar.input(nar.task("motor(right)! " + tNeg));
//                } else if (v < 0.5f) {
//                    nar.input(nar.task("motor(left)! " + tNeg));
//                    nar.input(nar.task("motor(right)! " + tPos));
//                } else if (v < 0.75f) {
//                    nar.input(nar.task("motor(forward)! " + tPos));
//                    nar.input(nar.task("motor(reverse)! " + tNeg));
//                } else {
//                    nar.input(nar.task("motor(forward)! " + tNeg));
//                    nar.input(nar.task("motor(reverse)! " + tPos));
//                }
//                return desire;
//            }
//        };
//        /*new CycleDesire("motor(forward)", strongestTask, nar) {
//            @Override
//            float onFrame(float desire) {
//                thrustRelative(desire * linearThrustPerCycle);
//                return desire;
//            }
//
//        };*/
//        /*new CycleDesire("motor(reverse)", strongestTask, nar) {
//            @Override float onCycle(float desire) {
//                thrustRelative(desire * -linearThrustPerCycle);
//                return desire;
//            }
//        };*/
//
//        new BiCycleDesire("motor(forward)", "motor(reverse)", strongestTask,nar) {
//
//            @Override
//            float onFrame(float desire, boolean positive) {
//                if (positive) {
//                    thrustRelative(desire * linearThrustPerCycle);
//                }
//                else {
//                    thrustRelative(desire * -linearThrustPerCycle);
//                }
//                return desire;
//            }
//        };
//
//        new BiCycleDesire("motor(left)", "motor(right)", strongestTask,nar) {
//
//            @Override
//            float onFrame(float desire, boolean positive) {
//
//                if (positive) {
//                    rotateRelative(+80*desire);
//                }
//                else {
//                    rotateRelative(-80*desire);
//                }
//                return desire;
//            }
//        };
//
//        new CycleDesire("motor(left)", strongestTask, nar) {
//            @Override float onCycle(float desire) {
//
//                return desire;
//            }
//        };
//        new CycleDesire("motor(right)", strongestTask, nar) {
//            @Override float onCycle(float desire) {
//
//                return desire;
//            }
//        };
