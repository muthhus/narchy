/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.rover.robot;

import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.nal.Tense;
import nars.rover.Material;
import nars.rover.Sim;
import nars.rover.obj.NARVisionRay;
import nars.rover.obj.VisionRay;
import nars.term.Compound;
import nars.util.FloatSupplier;
import nars.util.data.Util;
import nars.op.NarQ;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

import static nars.util.Texts.n2;


/**
 * Triangular mobile vehicle
 */
public class NARover extends AbstractPolygonBot {

    public final NAR nar;

    //final Lobjects objs;

    float hungry, sick;
    final SensorConcept speedFore, speedBack, leftSpeed, rightSpeed,
            hungrySensor, sickSensor;

    public static final Compound EAT_FOOD = $.image(1, $.the("eat"), $.the("food") );
    public static final Compound EAT_POISON = $.image(1, $.the("eat"), $.the("poison") );
    public static final Compound SPEED_LEFT = $.image(1, $.the("speed"), $.the("left") );
    public static final Compound SPEED_RIGHT = $.image(1, $.the("speed"), $.the("right") );
    public static final Compound SPEED_FORE = $.image(1, $.the("speed"), $.the("fore") );
    public static final Compound SPEED_BACK = $.image(1, $.the("speed"), $.the("back") );


//    final SimpleAutoRangeTruthFrequency linearVelocity;
//    final SimpleAutoRangeTruthFrequency motionAngle;
//    final SimpleAutoRangeTruthFrequency facingAngle;

    //public class DistanceInput extends ChangedTextInput

    final static Logger logger = LoggerFactory.getLogger(NARover.class);

    //private MotorControls motors;
    private Turret gun;
    private BeingMaterial material;

    public NARover(String id, NAR nar) {
        super(id);

        this.nar = nar;

        material = new BeingMaterial(this);


        int minUpdateTime = 4;


        hungry = 1f;
        sick = 0f;

        FloatToFloatFunction speedThresholdToFreq = (speed) -> {
            return speed < 0.01 ? 0 : Util.clamp(0.5f + speed);
        };
        FloatToFloatFunction sigmoid = (n) -> {
            return Util.sigmoid(n);
        };
        FloatToFloatFunction sigmoidIfPositive = (n) -> {
            return n > 0 ? Util.sigmoid(n) : -1; //Float.NaN;
        };
        FloatToFloatFunction sigmoidIfNegative = (n) -> {
            return n < 0 ? Util.sigmoid(-n) : -1; //Float.NaN;
        };
        FloatToFloatFunction linearPositive = (n) -> {
            return n > 0 ? Util.clamp(n) : -1; //Float.NaN;
        };
        FloatToFloatFunction linearNegative = (n) -> {
            return n < 0 ? Util.clamp(-n) : -1; //Float.NaN;
        };

        Vec2 forwardVec = new Vec2(1,0f);
        Vec2 tmp = new Vec2(), tmp2 = new Vec2();
        FloatSupplier linearSpeed =
                () -> {

                    //float thresh = 0.01f;

                    Vec2 lv = torso.getLinearVelocityFromLocalPointToOut(Vec2.ZERO, tmp2);
                    Vec2 worldForward = torso.getWorldPointToOut(forwardVec, tmp).subLocal(torso.getWorldCenter());
                    float v = Vec2.dot(
                            lv,
                            worldForward
                    );

                    //System.out.println("linear vel=" + v);

                    v *= 0.35f;

                    /*if (Math.abs(v) < thresh)
                        v = 0;*/
                    return v;
                };


        this.speedFore = new SensorConcept(SPEED_FORE, nar, linearSpeed, linearPositive)
                .timing(minUpdateTime, 0);

        this.speedBack = new SensorConcept(SPEED_BACK, nar, linearSpeed, linearNegative)
                .timing(minUpdateTime, 0);




        FloatSupplier angleSpeed = () -> {
            float v = torso.getAngularVelocity();

            //System.out.println("angle vel=" + angularVelocity);

            v *= 1f; //sensitivity

            return v;
        };

        this.leftSpeed = new SensorConcept(SPEED_LEFT, nar, angleSpeed, linearNegative)
            .timing(minUpdateTime, 0);

        this.rightSpeed = new SensorConcept(SPEED_RIGHT, nar, angleSpeed, linearPositive)
            .timing(minUpdateTime, 0);

        hungrySensor = new SensorConcept(EAT_FOOD, nar, () -> 1f-hungry, linearPositive)
            .timing(minUpdateTime, 0);

        sickSensor = new SensorConcept(EAT_POISON, nar, () -> sick, linearPositive)
            .timing(minUpdateTime, 0);

        float motorThresh = 0.1f;

        MotorConcept motorLeft = new MotorConcept("motor(left)", nar, (a) -> {
            if (a < motorThresh) return 0;
            return angularThrust(a);
        });

        MotorConcept motorRight = new MotorConcept("motor(right)", nar, (a) -> {
            if (a < motorThresh) return 0;
            return angularThrust(-a);
        });

        MotorConcept motorFore = new MotorConcept("motor(fore)", nar, (l) -> {
            if (l < motorThresh) return 0;
            return linearThrust(l);
        });

        MotorConcept motorBack = new MotorConcept("motor(back)", nar, (l) -> {
            if (l < motorThresh) return 0;
            return linearThrust(-l);
        });

        MotorConcept motorStop = new MotorConcept("motor(stop)", nar, (s) -> {
            if (s < motorThresh) return 0;
            stop(s);
            return s;
        });

    }

    @Override
    protected void onEat(Body eaten, Material m) {
        if (m instanceof Sim.FoodMaterial) {
            logger.warn("food");
            //nar.input("eat:food. :|: %1.0;0.9%");
            hungry = 0;

            //nar.input("goal:{food}. :|: %1.00;0.75%");
            //nar.input("goal:{health}. :|: %1.00;0.75%");
        }
        else if (m instanceof Sim.PoisonMaterial) {
            logger.warn("poison");
            //nar.input("eat:poison. :|:");
            sick = Util.clamp(sick + 1f);

            //nar.input("goal:{food}. :|: %0.00;0.90%");
            //nar.input("goal:{health}. :|: %0.00;0.90%");
        }

    }

    @Override
    long time() {
        return nar.time();
    }

    @Override
    public void step(int dt) {
        super.step(dt);

        gun.step(dt);

        try {
            nar.step();
        } catch (RuntimeException e) {
            e.printStackTrace();
            nar.stop();
        }

        sick = Util.clamp(sick - 0.002f);
        hungry = Util.clamp(hungry + 0.002f);
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

                nar.goal(EAT_FOOD, Tense.Present, 1f, 0.9f);//"eat:food! %1.00|0.95%");
                nar.goal(EAT_POISON, Tense.Present, 0f, 0.9f);

                //nar.input("speed:forward! %1.00;0.7%");
                //nar.input("eat:poison! %0.0|0.9%");
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


//    protected void train(long t) {
//        //float freq = 0.5f + 0.5f * (1/(1f + t/5000f)), conf = 0.85f;
//        float freq = 1f, conf = 0.9f;
//        //nar.input("MotorControls(random,motor,(),#x)! %1.0;0.1%");
//        nar.input("MotorControls(random,motor,(),#x)! %" + n2(freq) + "|" + n2(conf) + "%");
//        System.out.println("@" + t + " Curiosity Trained @ freq=" + freq);
//    }

    @Override
    public void init(Sim p) {
        super.init(p);

        gun = new Turret(sim, this);
    }

    @Override
    public BeingMaterial getMaterial() {
        return material;
    }

    @Override
    protected Body newTorso() {

        Body torso = newTriangle(getWorld(), mass);

        torso.setUserData(getMaterial());

        return torso;
    }


    /** http://stackoverflow.com/questions/20904171/how-to-create-snake-like-body-in-box2d-and-cocos2dx */
    public Arm addArm(Being b, String id, NarQ controller, float ax, float ay, float angle) {

        int segs = 4;
        float segLength = 3.7f;
        float thick = 1.5f;

        Arm a = new Arm(id, sim, torso, ax, ay, angle, segs, segLength, thick);

        //addEye(b, id + "e", controller, a.segments.getLast(), 9, new Vec2(0.5f, 0), 1f, (float)(+Math.PI/4f), 2.5f);

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




    public List<SensorConcept> addEye(Being b, String id, NarQ controller, Body base, int detail, Vec2 center, float arc, float centerAngle, float distance) {
        return addEye(b, id, controller, base, 1, detail, center, arc, centerAngle, distance, (v) -> {});
    }
    public List<SensorConcept> addEyeWithMouth(Being b, String id, NarQ controller, Body base, int pixels, int detail, Vec2 center, float arc, float centerAngle, float distance, float mouthArc) {
        return addEye(b, id, controller, base, pixels, detail, center, arc, centerAngle, distance, (v) -> {
            //float angle = v.angle;
            v.setEats(true);
                    //((angle < mouthArc / 2f) || (angle > (Math.PI * 2f) - mouthArc / 2f)));
        });
    }

    public List<SensorConcept> addEye(Being b, String id, NarQ controller, Body base, int pixels, int resolution, Vec2 center,
                                      float arc, float centerAngle, float distance, Consumer<VisionRay> each) {

        //final float twoPi = (float)Math.PI * 2f;
        float aStep = arc/pixels;

        float startAngle = centerAngle - arc/2;

        //final MutableFloat servo = new MutableFloat();


        List<SensorConcept> sensorConcepts = Global.newArrayList(pixels * resolution);

        String[] materials = {"food", "poison", "wall"};
        float pixelPri = 1f/(materials.length * pixels);


        for (int i = 0; i < pixels; i++) {
            final float angle = (startAngle + (aStep * i));

            NARVisionRay v = new NARVisionRay(id, i, nar, base, center, angle, aStep,
                    resolution, distance);


//                  @Override public float getLocalAngle () {
//                      return 0.5f * (float)Math.sin(servo.floatValue()); //controller.get();
//                  }

            each.accept(v);

            for (String material : materials) {

                DoubleSupplier value = () -> {
                    if (v.hit(material)) {
                        float x = (1f - v.seenDist); //closer = larger number (up to 1.0)
                        if (x < 0.1f) return 0;
                        else return 0.5f + 0.45f * x;
                    }
                    return 0; //nothing seen within the range
                };


                Compound term = $.imageInt(1, v.visionTerm, $.the(material));

                SensorConcept visionSensor = new SensorConcept(

                        //$.prop(v.visionTerm, $.the(material)),
                        //$.p(v.visionTerm, $.the(material)),
                        term,

                        nar,

                        () -> (float) value.getAsDouble()

                ).resolution(0.08f).timing(1, 8).
                        pri(pixelPri);
                sensorConcepts.add(visionSensor);

                controller.input.add(value);
            }

            b.getMaterial().layers.add(v);

            senses.add(v);
        }

        return sensorConcepts;


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


    //protected void addMotorController() throws Exception {
        //motors = objs.the("motor", MotorControls.class, this);
    //}

    //public static final ConceptDesire strongestTask = (c ->  c.getGoals().topEternal().getExpectation() );


    @Deprecated @Override
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

//    public static class MotorControls {
//
//        public final NARover rover;
//        private final Termed left, right, stop, forward;
//        private final Termed backward;
//        final boolean proportional = true; //thrust proportional to expectation of desire
//
//        public MotorControls(NARover rover) {
//            this.rover = rover;
//
//            forward = rover.nar.term(SomeRovers.motorForward);
//            backward = rover.nar.term(SomeRovers.motorBackward);
//            left = rover.nar.term(SomeRovers.motorLeft);
//            right = rover.nar.term(SomeRovers.motorRight);
//            stop = rover.nar.term(SomeRovers.motorStop);
//        }
//
//        public Truth stop() {
//            Task c = MethodOperator.invokingTask();
//            float strength = (proportional && c!=null) ? c.motivation() : 1;
//
//            return c.truth().confMult(rover.stop(strength)); //TODO use feedback discounting how much actually was stopped already
//        }
//
//        private Truth forward(boolean forward) {
//            Task c = MethodOperator.invokingTask();
//            float thrust = (proportional && c!=null) ? c.motivation() : 1;
//            if (!forward) thrust = -thrust;
//            rover.linear(thrust);
//            return c.truth();
//        }
//
//        private Truth rotate(boolean right) {
//            Task c = MethodOperator.invokingTask();
//            float thrust = (proportional && c!=null) ? c.motivation() : 1;
//            if (right) thrust = -thrust;
//            rover.rotateRelative(thrust);
//            return c.truth();
//        }
//
//        public Truth left() {  return rotate(false); }
//        public Truth right() {  return rotate(true); }
//        public Truth forward() {  return forward(true); }
//        public Truth backward() {  return forward(false); }
//
//
//        final static Truth unfired = new DefaultTruth(0.5f, 1f);
//
//        public Truth fire() {
//
//            Task c = MethodOperator.invokingTask();
//            if (rover.gun.fire(rover.torso, c.motivation())) {
//                return c.truth();
//            }
//            return unfired;
//        }
//
//        @Deprecated public Task random() {
//            Task c = MethodOperator.invokingTask();
//
//            Termed term;
//
//            switch ((int)(5 * Math.random())) {
//                case 0:
//                    term = stop; break;
//                case 1:
//                    term = forward; break;
//                case 2:
//                    term = backward; break;
//                case 3:
//                    term = left; break;
//                case 4:
//                    term = right; break;
//                default:
//                    term = null;
//            }
//
//            return new MutableTask(term, Symbols.GOAL)
//                    .budget(c.budget())
//                    .truth( c.truth() )
//                    .present(rover.nar)
//                    .log("Curiosity");
//        }
//    }



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
