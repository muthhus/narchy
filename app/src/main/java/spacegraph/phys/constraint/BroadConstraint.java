package spacegraph.phys.constraint;

import spacegraph.phys.Collidable;
import spacegraph.phys.collision.broad.Broadphase;

import java.util.List;


/** for applying NxN interactions */
public interface BroadConstraint {
    void solve(Broadphase b, List<Collidable> objects, float timeStep);
}
