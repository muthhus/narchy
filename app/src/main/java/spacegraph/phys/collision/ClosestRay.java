package spacegraph.phys.collision;

import spacegraph.math.v3;
import spacegraph.phys.Collisions;
import spacegraph.phys.math.Transform;
import spacegraph.phys.math.VectorUtil;

/**
 * Created by me on 7/22/16.
 */
public class ClosestRay extends Collisions.RayResultCallback {
    public final v3 rayFromWorld = new v3(); //used to calculate hitPointWorld from hitFraction
    public final v3 rayToWorld = new v3();

    public final v3 hitNormalWorld = new v3();
    public final v3 hitPointWorld = new v3();

    public ClosestRay(short group) {
        collisionFilterGroup = group;
    }

    public ClosestRay(v3 rayFromWorld, v3 rayToWorld) {
        set(rayFromWorld, rayToWorld);
    }

    public ClosestRay set(v3 rayFromWorld, v3 rayToWorld) {
        this.rayFromWorld.set(rayFromWorld);
        this.rayToWorld.set(rayToWorld);
        hitNormalWorld.zero();
        hitPointWorld.zero();
        closestHitFraction = 1f;
        collidable = null;
        return this;
    }

    @Override
    public float addSingleResult(Collisions.LocalRayResult rayResult, boolean normalInWorldSpace) {
        // caller already does the filter on the closestHitFraction
        assert (rayResult.hitFraction <= closestHitFraction);

        closestHitFraction = rayResult.hitFraction;
        collidable = rayResult.collidable;
        if (normalInWorldSpace) {
            hitNormalWorld.set(rayResult.hitNormal);
        } else {
            // need to transform normal into worldspace
            hitNormalWorld.set(rayResult.hitNormal);
            collidable.getWorldTransform(new Transform()).basis.transform(hitNormalWorld);
        }

        VectorUtil.setInterpolate3(hitPointWorld, rayFromWorld, rayToWorld, rayResult.hitFraction);
        return rayResult.hitFraction;
    }
}
