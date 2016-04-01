package nars.rover.run;

import nars.rover.Sim;
import nars.rover.world.FoodSpawnWorld1;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import org.jbox2d.dynamics.joints.WeldJointDef;

import java.util.ArrayList;
import java.util.List;

/**
 * http://rednuht.org/genetic_walkers/js/walker.js
 * http://rednuht.org/geneticat/js/cat.js
 */
public class DemoHumanoid {

    static float scale = 12;
    static float torsoUpperWidth = 0.25f * scale;
    static float torsoUpperHeight = 0.45f * scale;

    static float torsoLowerWidth = 0.25f * scale;
    static float torsoLowerHeight = 0.2f * scale;

    static float femur_width = 0.18f * scale;
    static float femur_length = 0.45f * scale;
    static float tibia_width = 0.13f * scale;
    static float tibia_length = 0.38f * scale;
    static float foot_height = 0.08f * scale;
    static float foot_length = 0.28f * scale;

    static float arm_width = 0.12f * scale;
    static float arm_length = 0.37f * scale;
    static float forearm_width = 0.1f * scale;
    static float fore_arm_length = 0.42f * scale;

    static float head_width = 0.22f * scale;
    static float head_height = 0.22f * scale;
    static float neck_width = 0.1f * scale;
    static float neck_height = 0.08f * scale;

    static BodyDef bd = new BodyDef();
    static FixtureDef fd = new FixtureDef();
    
    static List<Joint> joints = new ArrayList();

    public static void main(String[] args) {

        final Sim sim = new Sim(new World2D());
        new FoodSpawnWorld1(sim, 128, 48, 48, 0.5f);


        float density = 106.2f; // common for all fixtures, no reason to be too specific


        bd.type = BodyType.DYNAMIC;
        bd.linearDamping = 0;
        bd.angularDamping = 0.01f;
        bd.allowSleep = true;
        bd.awake = true;

        fd.density = density;
        fd.restitution = 0.1f;
        fd.shape = new PolygonShape();
        fd.filter.groupIndex = -1;



        Body[] torso = createTorso(sim.world);
        Body[] left_leg = createLeg( sim.world);
        Body[] right_leg = createLeg( sim.world);
        Body[] left_arm = createArm( sim.world);
        Body[] right_arm = createArm( sim.world);
        Body[] head = createHead( sim.world);
        connectParts(sim.world, head[0], torso, left_arm, right_arm, left_leg, right_leg);

        sim.world.setGravity(new Vec2(0,-5.5f));

        /*Entity mrnars = sim.game.createEntity().edit()
                .add(new Physical(
                        AbstractPolygonBot.newDynamic(0, 0),
                        AbstractPolygonBot.newTriangle()))

                .getEntity();*/

        //RoverWorld world = new ReactorWorld(32, 48, 32);
        //new FoodSpawnWorld1(sim, 128, 48, 48, 0.5f);

        sim.run(25);

    }

    static Body[] createTorso(World2D world) {
        // upper torso
        bd.position.set(0.5f - foot_length/2f + tibia_width/2f, foot_height/2f + foot_height/2f + tibia_length + femur_length + torsoLowerHeight + torsoUpperHeight/2f);
        Body upper_torso = world.createBody(bd);

        fd.setShape(new PolygonShape().setAsBox(torsoUpperWidth/2f, torsoUpperHeight/2f));
        upper_torso.createFixture(fd);

        // lower torso
        bd.position.set(0.5f - foot_length/2f + tibia_width/2f, foot_height/2f + foot_height/2f + tibia_length + femur_length + torsoLowerHeight/2f);
        Body lower_torso = world.createBody(bd);

        fd.setShape(new PolygonShape().setAsBox(torsoLowerWidth/2f, torsoLowerHeight/2f));
        lower_torso.createFixture(fd);

        // torso joint
        RevoluteJointDef jd = new RevoluteJointDef();
        Vec2 position = upper_torso.getPosition().clone();
        position.y -= torsoUpperHeight/2f;
        position.x -= torsoUpperWidth/3;
        jd.initialize(upper_torso, lower_torso, position);
        jd.lowerAngle = (float)-Math.PI/18f;
        jd.upperAngle = (float)Math.PI/10f;
        jd.enableLimit = true;
        jd.maxMotorTorque = 250f;
        jd.motorSpeed = 0f;
        jd.enableMotor = true;
        joints.add(world.createJoint(jd));

        return new Body[] { upper_torso, lower_torso };
    }

    static Body[] createLeg(World2D world) {

        // upper leg
        bd.position.set(0.5f - foot_length/2f + tibia_width/2f, foot_height/2f + foot_height/2f + tibia_length + femur_length/2f);
        Body upper_leg = world.createBody(bd);

        fd.setShape(new PolygonShape().setAsBox(femur_width/2f, femur_length/2f));
        upper_leg.createFixture(fd);

        // lower leg
        bd.position.set(0.5f - foot_length/2f + tibia_width/2f, foot_height/2f + foot_height/2f + tibia_length/2f);
        Body  lower_leg = world.createBody(bd);

        fd.setShape(new PolygonShape().setAsBox(tibia_width/2f, tibia_length/2f));
        lower_leg.createFixture(fd);

        // foot
        bd.position.set(0.5f, foot_height/2f);
        Body foot = world.createBody(bd);

        fd.setShape(new PolygonShape().setAsBox(foot_length/2f, foot_height/2f));
        foot.createFixture(fd);

        RevoluteJointDef jd = new RevoluteJointDef();

        // leg joints
        {
            Vec2 position = upper_leg.getPosition().clone();
            position.y -= femur_length / 2f;
            position.x += femur_width / 4;
            jd.initialize(upper_leg, lower_leg, position);
            jd.lowerAngle = -1.6f;
            jd.upperAngle = -0.2f;
            jd.enableLimit = true;
            jd.maxMotorTorque = 160;
            jd.motorSpeed = 0;
            jd.enableMotor = true;
            joints.add(world.createJoint(jd));
        }

        // foot joint
        {
            Vec2 position = lower_leg.getPosition().clone();
            position.y -= tibia_length / 2f;
            jd.initialize(lower_leg, foot, position);
            jd.lowerAngle = (float)-Math.PI / 5f;
            jd.upperAngle = (float)Math.PI / 6f;
            jd.enableLimit = true;
            jd.maxMotorTorque = 70;
            jd.motorSpeed = 0;
            jd.enableMotor = true;
            joints.add(world.createJoint(jd));
        }

        return new Body[] { upper_leg, lower_leg, foot };
    }

    static Body[] createArm(World2D world) {
        // upper arm
        bd.position.set(0.5f - foot_length/2f + tibia_width/2f, foot_height/2f + foot_height/2f + tibia_length + femur_length + torsoLowerHeight + torsoUpperHeight - arm_length/2f);
        Body upper_arm = world.createBody(bd);

        fd.setShape(new PolygonShape().setAsBox(arm_width/2f, arm_length/2f));
        upper_arm.createFixture(fd);

        // lower arm
        bd.position.set(0.5f - foot_length/2f + tibia_width/2f, foot_height/2f + foot_height/2f + tibia_length + femur_length + torsoLowerHeight + torsoUpperHeight - arm_length - fore_arm_length /2f);
        Body  lower_arm = world.createBody(bd);

        fd.setShape(new PolygonShape().setAsBox(forearm_width/2f, fore_arm_length /2f));
        lower_arm.createFixture(fd);

        // arm joint
        RevoluteJointDef jd = new RevoluteJointDef();
        Vec2 position = upper_arm.getPosition().clone();
        position.y -= arm_length/2f;
        jd.initialize(upper_arm, lower_arm, position);
        jd.lowerAngle = 0;
        jd.upperAngle = 1.22f;
        jd.enableLimit = true;
        jd.maxMotorTorque = 60;
        jd.motorSpeed = 0;
        jd.enableMotor = true;
        joints.add(world.createJoint(jd));

        return new Body[] {upper_arm, lower_arm};
    }

    static Body[] createHead(World2D world) {
        // neck
        bd.position.set(0.5f - foot_length/2f + tibia_width/2f, foot_height/2f + foot_height/2f + tibia_length + femur_length + torsoLowerHeight + torsoUpperHeight + neck_height/2f);
        Body neck = world.createBody(bd);

        fd.setShape(new PolygonShape().setAsBox(neck_width/2f, neck_height/2f));
        neck.createFixture(fd);

        // head
        bd.position.set(0.5f - foot_length/2f + tibia_width/2f, foot_height/2f + foot_height/2f + tibia_length + femur_length + torsoLowerHeight + torsoUpperHeight + neck_height + head_height/2f);
        Body head = world.createBody(bd);

        fd.setShape(new PolygonShape().setAsBox(head_width/2f, head_height/2f));
        head.createFixture(fd);

        // neck joint
        RevoluteJointDef jd = new RevoluteJointDef();
        Vec2 position = neck.getPosition().clone();
        position.y += neck_height /2f;
        jd.initialize(head, neck, position);
        jd.lowerAngle = -0.1f;
        jd.upperAngle = 0.1f;
        jd.enableLimit = true;
        jd.maxMotorTorque = 2;
        jd.motorSpeed = 0;
        jd.enableMotor = true;
        joints.add(world.createJoint(jd));

        return new Body[] {head, neck};
    }

    static void connectParts(World2D world, Body neck,
                             Body[] torso,
                             Body[] left_arm, Body[] right_arm,
                             Body[] left_leg, Body[] right_leg
    ) {
        // neck/torso
//   RevoluteJointDef jd = new RevoluteJointDef();
//   Vec2 position = head.neck.getPosition().clone();
//   position.y -= neck_height/2f;
//   jd.initialize(head.neck, torso.upper_torso, position);
//   jd.lowerAngle = 0;
//   jd.upperAngle = 0.2;
//   jd.enableLimit = true;
//   jd.maxMotorTorque = 2;
//   jd.motorSpeed = 0;
//   jd.enableMotor = true;
//   joints.add(world.createJoint(jd));

        Body upper_torso = torso[0];
        Body lower_torso = torso[1];

        //neck/torso
        {
            WeldJointDef jd = new WeldJointDef();
            jd.bodyA = neck;
            jd.bodyB = upper_torso;
            jd.localAnchorA = new Vec2(0, -neck_height / 2f);
            jd.localAnchorB = new Vec2(0, torsoUpperHeight / 2f);
            jd.referenceAngle = 0;
            world.createJoint(jd);
        }

        Vec2 position = upper_torso.getPosition().clone();

        // torso/arms
        {
            RevoluteJointDef jd = new RevoluteJointDef();
            position.y += torsoUpperHeight / 2f;
            jd.initialize(upper_torso, right_arm[0], position);
            jd.lowerAngle = (float) -Math.PI / 5;
            jd.upperAngle = (float) Math.PI / 4;
            jd.enableLimit = true;
            jd.maxMotorTorque = 120;
            jd.motorSpeed = 0;
            jd.enableMotor = true;
            joints.add(world.createJoint(jd));
        }

        {
            RevoluteJointDef jd = new RevoluteJointDef();
            jd.initialize(upper_torso, left_arm[0], position);
            jd.lowerAngle = (float) -Math.PI / 5;
            jd.upperAngle = (float) Math.PI / 4;
            jd.enableLimit = true;
            jd.maxMotorTorque = 120;
            jd.motorSpeed = 0;
            jd.enableMotor = true;
            joints.add(world.createJoint(jd));
        }

        // torso/legs
        {
            RevoluteJointDef jd = new RevoluteJointDef();
            position = lower_torso.getPosition().clone();
            position.y -= torsoLowerHeight / 2f;
            jd.initialize(lower_torso, right_leg[0], position);
            jd.lowerAngle = (float) -Math.PI / 7;
            jd.upperAngle = (float) Math.PI / 6;
            jd.enableLimit = true;
            jd.maxMotorTorque = 250;
            jd.motorSpeed = 0;
            jd.enableMotor = true;
            joints.add(world.createJoint(jd));
        }

        {
            RevoluteJointDef jd = new RevoluteJointDef();
            jd.initialize(lower_torso, left_leg[0], position);
            jd.lowerAngle = (float) -Math.PI / 7;
            jd.upperAngle = (float) Math.PI / 6;
            jd.enableLimit = true;
            jd.maxMotorTorque = 250;
            jd.motorSpeed = 0;
            jd.enableMotor = true;
            joints.add(world.createJoint(jd));
        }
    }

}
