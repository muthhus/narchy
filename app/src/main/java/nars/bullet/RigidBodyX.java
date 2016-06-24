package nars.bullet;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;


public class RigidBodyX extends RigidBody {

    public RigidBodyX(float mass, MotionState motionState, CollisionShape collisionShape) {
        super(mass, motionState, collisionShape);
    }

    public RigidBodyX(RigidBodyConstructionInfo rigidBodyConstructionInfo) {
        super(rigidBodyConstructionInfo);
    }

    public final Transform transform() {
        return worldTransform;
    }

}
