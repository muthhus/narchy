package nars.gui.graph;

import spacegraph.SimpleSpatial;
import spacegraph.Spatial;
import spacegraph.layout.ForceDirected;
import spacegraph.phys.Collidable;
import spacegraph.phys.collision.broad.Broadphase;

import java.util.List;

public class EdgeDirected extends ForceDirected {

    @Override
    public void solve(Broadphase b, List<Collidable> objects, float timeStep) {

        float a = attraction.floatValue();

        for (int i = 0, objectsSize = objects.size(); i < objectsSize; i++) {
            Collidable c = objects.get(i);

            Spatial A = ((Spatial) c.data());

            //TODO abstract the Edges as a feature to optionally add to a TermWidget, not just for ConceptWidgets
            if (A instanceof TermWidget) {
                ((TermWidget<?>) A).edges().forEach(e -> {

                    float attraction = e.attraction;
                    if (attraction > 0) {
                        SimpleSpatial B = e.id;

                        if ((B.body != null)) {

                            attract(c, B.body, a * attraction, e.attractionDist);
                        }
                    }

                });
            }


        }

        super.solve(b, objects, timeStep);
    }
}
