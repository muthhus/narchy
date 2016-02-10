package nars.rover.robot;

import nars.op.meta.HaiQ;
import nars.rover.Sim;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.physics.j2d.SwingDraw;
import nars.util.data.Util;
import nars.util.data.list.FasterList;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import org.jbox2d.dynamics.joints.WeldJoint;
import org.jbox2d.dynamics.joints.WeldJointDef;

import java.util.Arrays;

/**
 * Created by me on 2/10/16.
 */
public class Arm extends Robotic implements SwingDraw.LayerDraw {

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
    private final float wiggle;

    /**
     * absolute angle but relative to the attached base's angle
     */
    private float thetaTarget;

    /**
     * as a proportion of the armspan
     */
    private float radiusTarget;

    public Arm(String id, Sim sim, Body base, float ax, float ay, float ang,
               int segs, float segLength, float thick
    ) {
        super(((RoboticMaterial) base.getUserData()).clone().getID() + "_" + id);


        this.base = base;

        ((JoglAbstractDraw)sim.draw()).addLayer(this);

        Vec2 attachPoint = new Vec2(ax, ay);

        float vx = 1; //(float)Math.cos(a);
        float vy = 0; ///(float)Math.sin(a);

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
        );
        controller.setQ(0.25f, 0.5f, 0.7f, 0.1f);

        this.input = new float[controller.inputs()];


        jointRange = 2.5f; //joint flexibility
        wiggle = 0.03f;

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

            armSpan += dx;

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


    }


    public void set(float theta, float radius) {
        this.thetaTarget = theta;
        this.radiusTarget = radius;
    }

    transient final Vec2 a = new Vec2(), b = new Vec2();

    @Override
    public void step(int i) {
        super.step(i);

        //float pi = (float) (Math.PI);

        int k = 0;
        for (RevoluteJoint r : joints) {
            float a = (r.getJointAngle()) / (jointRange/2f); //-1..+1
            input[k++] = a;
        }
        input[k++] = MathUtils.reduceAngle(thetaTarget);
        input[k++] = radiusTarget;


        //the polar-specified point relative to the shoulder
        a.set(segments.getFirst().getWorldCenter());
        float baseAngle = base.getAngle();
        a.x += Math.cos(thetaTarget + baseAngle) * radiusTarget * armSpan;
        a.y += Math.sin(thetaTarget + baseAngle) * radiusTarget * armSpan;


        //where the end of the arm actually is
        b.set(segments.getLast().getWorldCenter());

        input[k++] = (b.x - a.x) / armSpan;
        input[k++] = (b.y - a.y) / armSpan;


        float reward = 0.25f + /* bias */
                -(b.sub(a).length() / (armSpan / 2f)); /* maybe circumference of armSpan? */

        reward = (float)Util.sigmoid(reward) - 0.5f;
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
        draw.drawCircle(a, 1f, Color3f.GREEN);
    }
}
