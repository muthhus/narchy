package nars.rover.robot;

import nars.Global;
import nars.op.meta.HaiQ;
import nars.rover.Sim;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.physics.j2d.LayerDraw;
import nars.util.data.Util;
import nars.util.data.list.FasterList;
import nars.util.signal.NarQ;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;

import java.util.List;

/**
 * Created by me on 2/10/16.
 */
public class Arm extends Robotic implements LayerDraw {

    final FasterList<Body> segments = new FasterList();
    final FasterList<RevoluteJoint> joints = new FasterList();
    final HaiQ controller;

    /**
     * to which it is attached (shoulder)
     */
    private final Body base;
    private final float[] input;
    private final float armSpan;
    private final float jointRange;

    /** base angle, angle relative to base */
    private final float ang;

    private float reward; //current reward value

    //private final float wiggle;

    public static class BoundedMutableFloat extends MutableFloat {

        float min, max;

        public BoundedMutableFloat(float v, float min, float max) {
            super();
            this.min = min;
            this.max = max;
            setValue(v);
        }


        @Override
        public void setValue(float value) {
            if (value < min) value = min;
            if (value > max) value = max;
            super.setValue(value);
        }
    }
    /**
     * proportion of the jointRange angle but relative to the attached base's angle
     */
    public final MutableFloat thetaTarget = new BoundedMutableFloat(0, -1, 1);

    /**
     * as a proportion of the armspan (0 = root, 1 = armspan radius)
     */
    public final MutableFloat radiusTarget = new BoundedMutableFloat(0.5f, 0.5f, 1f);

    public final List<NarQ.Action> controls;

    public Arm(String id, Sim sim, Body base, float ax, float ay, float ang,
               int segs, float segLength, float thick
    ) {
        super(((RoboticMaterial) base.getUserData()).clone().getID() + "_" + id);


        this.ang = ang;
        this.base = base;

        ((JoglAbstractDraw)sim.draw()).addLayer(this);

        //Vec2 attachPoint = new Vec2(ax, ay);


        // NOW, create several duplicates of the "Main Body" fixture
        // but offset them from the previous one by a fixed amount and
        // overlap them a bit.

        Body pBodyA = base;
        Body pBodyB = null;


        /* inputs:
                servo angle of each joint (normalized to angle limits)
                target polar theta and radius
                delta x, y of current position to target, both independently normalized to armLength
                */
        controller = new HaiQ((segs) + 2 + 2 ,
                (2+segs) * 6 /* arbitrary # states */,
                (segs) * 2 //forward and reverse motor impulse for each joint
                    // TODO: (1+segs) + 1 //segment select (including a position for none), and a direction select
        );
        controller.setQ(0.25f, 0.5f, 0.7f, 0.1f);

        this.input = new float[controller.inputs()];


        jointRange = 2.5f; //joint flexibility
        //wiggle = 0.03f;

        float armSpan = 0f;

        for (int idx = 0; idx < segs; idx++) {
            // Create a body for the next segment.

            float dx;

                dx = segLength;
                segLength *= 0.75f; //decrease subsequent
                thick *= 0.618f; // golden ratio, see: dan winter


            pBodyB = AbstractPolygonBot.newRect(sim.getWorld(), 0.2f,
                    dx, thick,
                    pBodyA.getPosition().x + dx, pBodyA.getPosition().y);

            armSpan += segLength;

            //float tone= (idx/((float)numSegments));

            pBodyB.setUserData(getMaterial());



            //pBodyB. position = pBodyA.getPosition().add( offset );
            //_segments.add(pBodyB);
            // Add some damping so body parts don't 'flop' around.
            pBodyB.setLinearDamping(0.1f);
            pBodyB.setAngularDamping(0.3f);

//            if (pBodyA == base)
//                pBodyB.getPosition().addLocal(attachPoint);

            // Create a Revolute Joint at a position half way
            // between the two bodies.
            Vec2 midpoint = //pBodyA.getPosition().add(pBodyB.getPosition()).mul(0.5f);
                pBodyB.getPosition().add(new Vec2(dx/2f, 0)); //this shuld be overrdden by the below code



                RevoluteJointDef revJointDef = new RevoluteJointDef();
                revJointDef.initialize(pBodyA, pBodyB, midpoint);

                if (pBodyA!=base) {
                    revJointDef.localAnchorA = new Vec2(+dx/2f, 0); //end of the block
                    revJointDef.localAnchorB = new Vec2(-dx/2f, 0); //start of the block
                } else {
                    revJointDef.localAnchorA = new Vec2(); //end of the block
                    revJointDef.localAnchorB = new Vec2(-dx/2f,0); //end of the block
                }
                revJointDef.collideConnected = false;
                revJointDef.enableLimit = true;
                revJointDef.enableMotor = true;
                revJointDef.upperAngle = +jointRange/2f;//
                // + (pBodyA == base ? ang : 0); //shoulder
                revJointDef.lowerAngle = -jointRange/2f;
                // + (pBodyA == base ? ang : 0); //shoulder

                RevoluteJoint jj = (RevoluteJoint) sim.getWorld().createJoint(revJointDef);
                joints.add(jj);


            segments.add(pBodyB);

            // Update so the next time through the loop, we are
            // connecting the next body to the one we just
            // created.
            pBodyA = pBodyB;


        }

        this.armSpan = armSpan;


        //adjust R and Theta targets

        controls = Global.newArrayList(4);
        addControls(thetaTarget);
        addControls(radiusTarget);

    }

    private void addControls(MutableFloat f) {
        float speed = 0.1f;
        for (float polarity: new float[] { -1, +1}) {
            this.controls.add(new NarQ.Action() {

                float last; //TODO calculate range bounds to reduce the actual applied strength

                @Override
                public void run(float strength) {
                    f.setValue(f.getValue() + speed*strength*polarity); //using setValue for its limiters
                    last = strength;
                }

                @Override
                public float ran() {
                    return last;
                }
            });
        }
    }


    public void set(float theta, float radius) {
        this.thetaTarget.setValue(theta);
        this.radiusTarget.setValue(radius);
    }

    transient final Vec2 a = new Vec2(), tWorld = new Vec2(), hWorld = new Vec2(),
            b = new Vec2();
    transient final Color3f targetColor = new Color3f(0,0,0);

    @Override
    public void step(int i) {
        super.step(i);

        //float pi = (float) (Math.PI);

        int k = 0;
        for (RevoluteJoint r : joints) {
            float a = (r.getJointAngle()) / (jointRange/2f); //-1..+1
            input[k++] = a;
        }
        input[k++] = thetaTarget.floatValue();
        input[k++] = radiusTarget.floatValue();


        //the polar-specified point relative to the shoulder
        a.set(segments.getFirst().getWorldCenter());

        float resultAngle = thetaTarget.floatValue() * (jointRange/2) + ang;
        float rad = Math.max(0f, radiusTarget.floatValue());
        this.a.x += (float)Math.cos(resultAngle) * rad * armSpan;
        this.a.y += (float)Math.sin(resultAngle) * rad * armSpan;


        //where the end of the arm actually is
        b.set(segments.getLast().getWorldCenter());

        input[k++] = (b.x - this.a.x) / armSpan;
        input[k++] = (b.y - this.a.y) / armSpan;


        float reward = 0.01f + /* bias, tolerance */
                -(b.sub(this.a).length() / (armSpan)); /* maybe circumference of armSpan? */

        this.reward = reward; //Util.sigmoid(reward) - 0.5f;

        int motor = controller.act(input, reward);
        float direction = (motor % 2 == 0) ? +1f : -1f;



        int motorJoint = motor / 2;
        float speed = 0.1f / (joints.size() - motorJoint); //inner joints slower because they affect the target more
        RevoluteJoint j = joints.get(motorJoint);


        //Mode 1: hard limits
        //float currentAngle = j.getJointAngle();
        //float targetAngle = currentAngle + direction * speed;
        //targetAngle = Math.min(targetAngle, -jointRange / 2f);
        //targetAngle = Math.max(targetAngle, jointRange / 2f);
        //j.setLimits(targetAngle-wiggle/2f, targetAngle+wiggle/2f);

        //Mode 2: relative impulse
        j.setLimits(-jointRange/2f, jointRange/2f);
        j.enableMotor(true);
        j.setMaxMotorTorque(10);
        j.setMotorSpeed(direction*speed);


        //j.motor



        //System.out.println(reward);
        //System.out.println(a + " " + b);
        //System.out.println(Arrays.toString(input) + "\t" + " <" + reward + "> " + motor + " " + direction);


    }

    @Override
    public RoboticMaterial getMaterial() {
        return ((RoboticMaterial) base.getUserData()).clone();
    }

    @Deprecated
    @Override
    protected Body newTorso() {
        return null;
    }

    @Override
    public void drawGround(JoglAbstractDraw draw, World w) {

    }

    @Override
    public void drawSky(JoglAbstractDraw draw, World w) {

        if (segments == null || segments.isEmpty()) return;

        hWorld.set( segments.getLast().getWorldCenter() );
        tWorld.set( base.getWorldCenter() );
        float resultAngle = thetaTarget.floatValue() * (jointRange/2) + base.getAngle() + ang;
        float rad = radiusTarget.floatValue();
        tWorld.x += (float)Math.cos(resultAngle) * rad * armSpan;
        tWorld.y += (float)Math.sin(resultAngle) * rad * armSpan;

        targetColor.set( Util.clamp( -reward), 0.5f, Util.clamp( reward ) );
        draw.drawCircle(tWorld, 0.5f, targetColor);

        draw.drawSegment(hWorld, tWorld, 0.5f, 0.5f, 0.5f, 0.8f, 2f);
    }
}
