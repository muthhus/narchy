package spacegraph.phys.collision.dispatch;

import spacegraph.phys.linearmath.Transform;
import spacegraph.phys.linearmath.VectorUtil;

import javax.vecmath.Vector3f;

/**
 * Created by me on 7/22/16.
 */
public class ClosestRay extends CollisionWorld.RayResultCallback {
    public final Vector3f rayFromWorld = new Vector3f(); //used to calculate hitPointWorld from hitFraction
    public final Vector3f rayToWorld = new Vector3f();

    public final Vector3f hitNormalWorld = new Vector3f();
    public final Vector3f hitPointWorld = new Vector3f();

    public ClosestRay(short group) {
        collisionFilterGroup = group;
    }

    public ClosestRay(Vector3f rayFromWorld, Vector3f rayToWorld) {
        set(rayFromWorld, rayToWorld);
    }

    public ClosestRay set(Vector3f rayFromWorld, Vector3f rayToWorld) {
        this.rayFromWorld.set(rayFromWorld);
        this.rayToWorld.set(rayToWorld);
        hitNormalWorld.zero();
        hitPointWorld.zero();
        closestHitFraction = 1f;
        collidable = null;
        return this;
    }

    @Override
    public float addSingleResult(CollisionWorld.LocalRayResult rayResult, boolean normalInWorldSpace) {
        // caller already does the filter on the closestHitFraction
        assert (rayResult.hitFraction <= closestHitFraction);

        closestHitFraction = rayResult.hitFraction;
        collidable = rayResult.collidable;
        if (normalInWorldSpace) {
            hitNormalWorld.set(rayResult.hitNormalLocal);
        } else {
            // need to transform normal into worldspace
            hitNormalWorld.set(rayResult.hitNormalLocal);
            collidable.getWorldTransform(new Transform()).basis.transform(hitNormalWorld);
        }

        VectorUtil.setInterpolate3(hitPointWorld, rayFromWorld, rayToWorld, rayResult.hitFraction);
        return rayResult.hitFraction;
    }
}
