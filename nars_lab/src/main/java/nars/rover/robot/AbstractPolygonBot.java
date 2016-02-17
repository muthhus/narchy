package nars.rover.robot;

import nars.NAR;
import nars.concept.Concept;
import nars.rover.Material;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.util.event.FrameReaction;
import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by me on 8/3/15.
 */
public abstract class AbstractPolygonBot extends Robotic {

//    private Consumer<Task> goalSolutionAnswered = (x)->{
//        $.logger.warn("Plan: {0}", x);
//    };

    public AbstractPolygonBot(String id) {
        super(id);
    }


    static float linearDamping = 0.8f;
    static float angularDamping = 0.6f;
    static float restitution = 0.9f; //bounciness
    static float friction = 0.5f;

    //final Deque<Vec2> positions = new ArrayDeque();
    protected final List<Sense> senses = new ArrayList();
    public float linearThrustPerCycle = 2*5f;
    public float angularSpeedPerCycle = 2*0.7f;
    int mission = 0;
    //public float curiosity = 0.1f;

    boolean feel_motion = true, feel_senses = true; //todo add option in gui
    int motionPeriod = 1, sensePeriod = 1;

    public static Body newTriangle(World world, float mass) {
        return newTriangle(world, mass, 0f, 0f);
    }

    @NotNull
    public static Body newTriangle(World world, float mass, float x, float y) {
        PolygonShape shape = new PolygonShape();

        Vec2[] vertices = {new Vec2(3.0f, 0.0f), new Vec2(-1.0f, +2.0f), new Vec2(-1.0f, -2.0f)};
        shape.set(vertices, vertices.length);
        //shape.m_centroid.set(bodyDef.position);
        BodyDef bd = new BodyDef();
        bd.linearDamping = (linearDamping);
        bd.angularDamping = (angularDamping);
        bd.type = BodyType.DYNAMIC;
        bd.position.set(x,y);

        Body torso = world.createBody(bd);
        Fixture f = torso.createFixture(shape, mass);
        f.setRestitution(restitution);
        f.setFriction(friction);
        return torso;
    }
    @NotNull
    public static Body newRect(World world, float density, float w, float h, float x, float y) {
        PolygonShape shape = new PolygonShape();

        float mass = density * w * h;

        w/=2;
        h/=2;

        Vec2[] vertices = {new Vec2(-1f*w, -1f*h), new Vec2(-1f*w, +1f*h), new Vec2(1f*w, 1f*h), new Vec2(1f*w, -1f*h), };
        shape.set(vertices, vertices.length);
        //shape.m_centroid.set(bodyDef.position);
        BodyDef bd = new BodyDef();
        bd.linearDamping = (linearDamping);
        bd.angularDamping = (angularDamping);
        bd.type = BodyType.DYNAMIC;
        bd.position.set(x,y);

        Body torso = world.createBody(bd);
        Fixture f = torso.createFixture(shape, mass);
        f.setRestitution(restitution);
        f.setFriction(friction);
        return torso;
    }

    public void thrustRelative(float f) {
        //float velBefore = torso.getLinearVelocity().length();
        if (f == 0) {
            torso.setLinearVelocity(new Vec2());
        } else {
            thrust(0, f * linearThrustPerCycle);
        }
        //float velAfter = torso.getLinearVelocity().length();
        //return new DefaultTruth(Util.sigmoidDiffAbs(velAfter, velBefore), 0.9f);
    }

    public void rotateRelative(float f) {
        //float vBefore = torso.getAngularVelocity();
        rotate(f * angularSpeedPerCycle);
        //float vAfter = torso.getAngularVelocity();

        //return new DefaultTruth(Util.sigmoidDiffAbs(vAfter, vBefore), 0.9f);
    }

    public void inputMission() {

    }


    public void taste(Body eatable, float distance) {
//        Rover2.Material m = (Rover2.Material)eatable.getUserData();
//        if (m instanceof Rover2.FoodMaterial) {
//            float c = 1.0f / (1.0f + (distance - biteDistanceThreshold) / (tasteDistanceThreshold - biteDistanceThreshold));
//            mouthInput.set("<goal --> food>. :|: %0." + (0.5f + c / 2f) + ";0." + (c / 2f) + "%");
//        }
    }

    protected void onEat(Body eaten, Material m) {

    }

    public void eat(Body eaten) {
        Material m = (Material)eaten.getUserData();

        onEat(eaten, m);

        @Deprecated int sz = 48;
        float x = (float) Math.random() * sz - sz / 2f;
        float y = (float) Math.random() * sz - sz / 2f;
        //random new position
        eaten.setTransform(new Vec2(x * 2.0f, y * 2.0f), eaten.getAngle());
    }

    public DebugDraw getDraw() {
        return draw;
    }


    @Override
    public void step(int time) {
        long now = sim.clock.time();
        if (now % sim.missionPeriod == 0) {
            inputMission();
        }

        if (feel_senses && (now % sensePeriod == 0)) {
            for (Sense v : senses) {
                v.step(true, true);
            }
        }
        /*if(cnt>=do_sth_importance) {
        cnt=0;
        do_sth_importance+=decrease_of_importance_step; //increase
        nar.addInput("(^motor,random)!");
        }*/
        if (feel_motion && now % motionPeriod == 0) {
            feelMotion();
        }
        /*if (Math.random() < curiosity) {
            randomAction();
        }*/

        /*if (feel_motion && now % sim.missionPeriod == 0) {
            System.out.println(nar.memory.emotion.happy() + " happy, " + nar.memory.emotion.busy() + " busy, " + nar.memory.index.size() + " concepts");
        }*/

    }

    public void thrust(float angle, float force) {
        angle += torso.getAngle();// + Math.PI / 2; //compensate for initial orientation
        //torso.applyForceToCenter(new Vec2((float) Math.cos(angle) * force, (float) Math.sin(angle) * force));
        Vec2 v = new Vec2((float) Math.cos(angle) * force, (float) Math.sin(angle) * force);
        //torso.setLinearVelocity(v);
        torso.applyLinearImpulse(v, torso.getWorldCenter(), true);
    }

    public void rotate(float v) {
        //torso.setAngularVelocity(v);
        torso.applyAngularImpulse(v);
        //torso.applyTorque(torque);
    }

    protected abstract void feelMotion();

    public void stop() {
        torso.setAngularVelocity(0);
        torso.setLinearVelocity(new Vec2());
    }

    @FunctionalInterface
    public interface ConceptDesire {
        float getDesire(Concept c);
    }


//    /** maps a scalar changing quality to a frequency value, with autoranging
//     *  determined by a history window of readings
//     * */
//    public static class AutoRangeTruthFrequency {
//        final NeuralGasNet net;
//        float threshold;
//
//        public AutoRangeTruthFrequency(float thresho) {
//            net = new NeuralGasNet(1,4);
//            this.threshold = thresho;
//        }
//
//        /** returns estimated frequency */
//        public float observe(float value) {
//            if (Math.abs(value) < threshold ) {
//                return 0.5f;
//            }
//
//            learn(value);
//
//            double[] range = net.getDimensionRange(0);
//            return (float) getFrequency(range, value);
//        }
//
//        protected double getFrequency(double[] range, float value) {
//            double proportional;
//
//            if ((range[0] == range[1]) || (!Double.isFinite(range[0])))
//                proportional = 0.5;
//            else
//                proportional = (value - range[0]) / (range[1] - range[0]);
//
//            if (proportional > 1f) proportional = 1f;
//            if (proportional < 0f) proportional = 0f;
//
//            return proportional;
//        }
//
//        protected void learn(float value) {
//            net.learn(value);
//        }
//
//
//    }
//
//    public static class BipolarAutoRangeTruthFrequency extends AutoRangeTruthFrequency {
//
//        public BipolarAutoRangeTruthFrequency() {
//            this(0);
//        }
//
//        public BipolarAutoRangeTruthFrequency(float thresh) {
//            super(thresh);
//        }
//
//        @Override protected void learn(float value) {
//            //learn the absolute value because the stimate will include the negative range as freq < 0.5f
//            super.learn(Math.abs(value));
//            super.learn(0);
//        }
//
//        protected double getFrequency(double[] range, float value) {
//            double proportional;
//
//            if ((0 == range[1]) || (!Double.isFinite(range[0])))
//                proportional = 0.5;
//            else
//                proportional = ((value) / (range[1]))/2f + 0.5f;
//
//            //System.out.println(value + "-> +-" + range[1] + " " + " -> " + proportional);
//
//            if (proportional > 1f) proportional = 1f;
//            if (proportional < 0f) proportional = 0f;
//
//            return proportional;
//        }
//    }
//
//    public static class SimpleAutoRangeTruthFrequency  {
//        private final Compound term;
//        private final NAR nar;
//        private final AutoRangeTruthFrequency model;
//
//        public SimpleAutoRangeTruthFrequency(NAR nar, Compound term, AutoRangeTruthFrequency model) {
//            super();
//            this.term = term;
//            this.nar = nar;
//            this.model = model;
//
//            model.net.setEpsW(0.04f);
//            model.net.setEpsN(0.01f);
//        }
//
//        public void observe(float value) {
//            float freq = model.observe(value);
//            //System.out.println(range[0] + ".." + range[1]);
//            //System.out.println(b);
//
//            float conf = 0.75f;
//
//            TaskSeed ts = nar.task(term).belief().present().truth(freq, conf);
//            Task t = ts.normalized();
//            if (t!=null)
//                nar.input(t);
//
//            //System.out.println(t);
//        }
//    }

    public abstract static class CycleDesire extends FrameReaction {

        private final ConceptDesire desireFunction;
        private final Term term;
        private final NAR nar;
        transient private Concept concept;
        boolean feedbackEnabled = true;
        float threshold = 0f;

        /** budget to apply if the concept is not active */
        //private final Budget remember = new UnitBudget(0.5f, Symbols.GOAL, new DefaultTruth(1.0f, 0.9f));

        public CycleDesire(String term, ConceptDesire desireFunction, NAR nar) {
            this(desireFunction, nar.term(term), nar);
        }

        public CycleDesire(ConceptDesire desireFunction, Term term, NAR nar) {
            super(nar);
            this.desireFunction = desireFunction;
            this.nar = nar;
            this.term = term;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '[' + concept + ']';
        }

//        public void setFeedback(boolean feedback) {
//            this.feedbackEnabled = feedback;
//        }

        public float getDesireIfConceptMissing() { return 0; }

        /** @return feedback belief value, or Float.NaN to not apply it */
        abstract float onFrame(float desire);

        @Override
        public void onFrame() {

            Concept c = getConcept();

            if (c != null) {
                float d = desireFunction.getDesire(c);

                if (d > threshold) {
                    float feedback = onFrame(d);

                    if (feedbackEnabled && Float.isFinite(feedback))
                        nar.input(getFeedback(feedback));
                }
            }
            else {
                onFrame(getDesireIfConceptMissing());
            }

        }

        public Task getFeedback(float feedback) {
            //since it's expectation, using 0.99 conf is like preserving the necessary truth as was desired, if feedback = desire
            return new MutableTask(term).present(nar.memory).belief().truth(feedback, 0.9f);
        }

        public Concept getConcept() {

            if (concept == null) {
                concept = nar.concept(term);
            }


            return concept;
        }
    }



    @FunctionalInterface
    public interface Sense {

        void step(boolean input, boolean draw);

    }


}
