package spacegraph;

import nars.gui.ConceptWidget;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.collision.broad.Broadphase;
import spacegraph.phys.util.OArrayList;

import java.util.List;

import static spacegraph.math.v3.v;

/**
 * Created by me on 8/24/16.
 */
public class ForceDirected<X> implements spacegraph.phys.constraint.BroadConstraint {

    public static final int clusters = 1;

    public float repelSpeed = 1f;
    public float attractSpeed = 2f;

    private final float minRepelDist = 0;
    private final float maxRepelDist = 100f;
    private final float attractDist = 1f;

//        public static class Edge<X> extends MutablePair<X,X> {
//            public final X a, b;
//            public Object aData;
//            public Object bData;
//
//            public Edge(X a, X b) {
//                super(a, b);
//                this.a = a;
//                this.b = b;
//            }
//        }
//
//        final SimpleGraph<X,Edge> graph = new SimpleGraph((a,b)->new Edge(a,b));
//
//        public Edge get(X x, X y) {
//            graph.addVertex(x);
//            graph.addVertex(y);
//            graph.getEdge(x, y);
//        }

    @Override
    public void solve(Broadphase b, OArrayList<Collidable> objects, float timeStep) {

        //System.out.print("Force direct " + objects.size() + ": ");
        //final int[] count = {0};
        b.forEach(objects.size() / clusters, objects, (l) -> {
            batch(l);
            //count[0] += l.size();
            //System.out.print(l.size() + "  ");
        });
        //System.out.println(" total=" + count[0]);

        for (Collidable c : objects) {

            Spatial A = ((Spatial) c.data());
            if (A instanceof ConceptWidget) {
                for (EDraw e : ((ConceptWidget) A).edges) {
                    if (e!=null) {

                        SimpleSpatial B = e.target;

                        if ((B != null) && (B != A) && (B.body != null)) {

                            float ew = e.width;
                            float attractStrength = ew * e.attraction;
                            attract(c, B.body, attractSpeed * attractStrength, attractDist);
                        }
                    }
                }
            }

        }

    }

    protected void batch(List<Collidable> l) {


        for (int i = 0, lSize = l.size(); i < lSize; i++) {
            Collidable x = l.get(i);
            for (int i1 = i + 1, lSize1 = l.size(); i1 < lSize1; i1++) {
                Collidable y = l.get(i1);

                repel(x, y, repelSpeed, minRepelDist, maxRepelDist);
            }
        }
    }

    private void attract(Collidable x, Collidable y, float speed, float idealDist) {
        SimpleSpatial xp = ((SimpleSpatial) x.data());
        SimpleSpatial yp = ((SimpleSpatial) y.data());

        v3 delta = v();
        delta.sub(xp.transform(), yp.transform());


        float len = delta.normalize();
        if (len <= 0)
            return;

        len -= (xp.radius + yp.radius);

        if (len > idealDist) {
            //float dd = (len - idealDist);
            float dd = 0; //no attenuation over distance

            delta.scale((-(speed * speed) / (1f + dd)) / 2f);

            ((Dynamic) x).impulse(delta);
            delta.negate();
            ((Dynamic) y).impulse(delta);

        }

    }

    private void repel(Collidable x, Collidable y, float speed, float minDist, float maxDist) {
        SimpleSpatial xp = ((SimpleSpatial) x.data());
        SimpleSpatial yp = ((SimpleSpatial) y.data());

        v3 delta = v();
        delta.sub(xp.transform(), yp.transform());

        float len = delta.normalize();
        len -= (xp.radius + yp.radius);

        if (len <= minDist)
            return;

        delta.scale(((speed * speed) / (1 + len * len)) / 2f);

        //experimental
//            if (len > maxDist) {
//                delta.negate(); //attract
//            }

        ((Dynamic) x).impulse(delta);
        //xp.moveDelta(delta, 0.5f);
        delta.negate();
        ((Dynamic) y).impulse(delta);
        //yp.moveDelta(delta, 0.5f);

    }


}
