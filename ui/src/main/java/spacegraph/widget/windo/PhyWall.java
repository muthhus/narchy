package spacegraph.widget.windo;

import spacegraph.Spatial;
import spacegraph.phys.Dynamics;
import spacegraph.phys.collision.DefaultCollisionConfiguration;
import spacegraph.phys.collision.DefaultIntersecter;
import spacegraph.phys.collision.broad.DbvtBroadphase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** wall which organizes its sub-surfaces according to 2D phys dynamics */
public class PhyWall {
    final Dynamics d;


    final Map<String, Spatial<String>> windoBodies = new ConcurrentHashMap<>();

    public PhyWall() {
        this.d = new Dynamics<String>(new DefaultIntersecter(new DefaultCollisionConfiguration()), new DbvtBroadphase(), windoBodies.values());
    }


}
