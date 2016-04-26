package nars.rover.system;

import com.artemis.Aspect;
import com.artemis.Entity;
import com.artemis.systems.EntityProcessingSystem;
import nars.rover.obj.Motorized;
import nars.rover.obj.Physical;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;

/**
 * Created by me on 3/29/16.
 */
public class MotorSystem extends EntityProcessingSystem {


    public MotorSystem() {
        super(Aspect.all(Physical.class, Motorized.class));
    }

    @Override
    protected void process(Entity e) {
        Body b = e.getComponent(Physical.class).body;
        Motorized p = e.getComponent(Motorized.class);

        float lin = p.fore - p.back;
        //lin = lin;
        //if (p.fore < p.back)
        //    lin *= -1;

        thrust(b, 0, lin * p.linearSpeed);

        rotate(b, (p.left - p.right) * p.angularSpeed);

        p.clear();
    }

    public void thrust(Body base, float angle, float force) {
        angle += base.getAngle();// + Math.PI / 2; //compensate for initial orientation
        //torso.applyForceToCenter(new Vec2((float) Math.cos(angle) * force, (float) Math.sin(angle) * force));
        Vec2 v = new Vec2((float) Math.cos(angle) * force, (float) Math.sin(angle) * force);
        //torso.setLinearVelocity(v);
        //torso.applyForceToCenter(v);
        base.applyLinearImpulse(v, base.getWorldCenter(), true);
    }

    public void rotate(Body base, float v) {
        //torso.setAngularVelocity(v);
        base.applyAngularImpulse(v);
        //torso.applyTorque(v);
    }

    public void stop(Body base, float strength) {


        //float speedBefore = base.getAngularVelocity() + base.getLinearVelocity().length();

        float brakes = (1f - strength);
        base.setAngularVelocity(base.getAngularVelocity() * brakes);
        base.setLinearVelocity( base.getLinearVelocity().mul(brakes));

        //float speedAfter = base.getAngularVelocity() + base.getLinearVelocity().length();

        //return strength * (speedAfter / (speedAfter + speedBefore));
    }

}
