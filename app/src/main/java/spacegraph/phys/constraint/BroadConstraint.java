package spacegraph.phys.constraint;

import spacegraph.phys.Collidable;
import spacegraph.phys.collision.broad.Broadphase;
import spacegraph.phys.util.OArrayList;


/** for applying NxN interactions */
public interface BroadConstraint {
    void solve(Broadphase b, OArrayList<Collidable> objects, float timeStep);
}
