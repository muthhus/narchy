package spacegraph.phys.constraint;

import spacegraph.phys.Collidable;
import spacegraph.phys.collision.broad.Broadphase;

import java.util.Collection;


/** for applying NxN interactions */
public interface BroadConstraint {
    void solve(Broadphase b, Collection<Collidable> objects, float timeStep);
}
