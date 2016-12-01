package spacegraph.layout;

import nars.gui.ConceptWidget;
import nars.util.Util;
import nars.util.data.FloatParam;
import spacegraph.SimpleSpatial;
import spacegraph.Spatial;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.collision.broad.Broadphase;

import java.util.List;

import static spacegraph.math.v3.v;

/**
 * Created by me on 8/24/16.
 */
public class ForceDirected implements spacegraph.phys.constraint.BroadConstraint {

    public static final int clusters =
            1;
            //13;


    public final FloatParam repel = new FloatParam(80, 0, 100);
    public final FloatParam attraction = new FloatParam(0.001f, 0, 5);

    final v3 boundsMin, boundsMax;
    final float maxRepelDist;

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

    public ForceDirected() {
        float r = 400;
        boundsMin = v(-r, -r, -r);
        boundsMax = v(+r, +r, +r);
        maxRepelDist = r*2.5f;

    }

    @Override
    public void solve(Broadphase b, List<Collidable> objects, float timeStep) {

        objects.forEach(c -> ((Spatial)c.data()).moveWithin(boundsMin, boundsMax));

        //System.out.print("Force direct " + objects.size() + ": ");
        //final int[] count = {0};
        //count[0] += l.size();
//System.out.print(l.size() + "  ");
        b.forEach((int) Math.ceil((float)objects.size() / clusters), objects, this::batch);
        //System.out.println(" total=" + count[0]);
        float a = attraction.floatValue();

        for (Collidable c : objects) {

            Spatial A = ((Spatial) c.data());
            if (A instanceof ConceptWidget) {
                ((ConceptWidget) A).edges.forEachKey(e -> {

                        ConceptWidget B = e.target;

                        if ((B.body != null)) {

                            attract(c, B.body, a * e.attraction, e.attractionDist);
                        }

                });
            }

        }



    }

    protected void batch(List<Collidable> l) {
        float speed = repel.floatValue();
        for (int i = 0, lSize = l.size(); i < lSize; i++) {
            Collidable x = l.get(i);
            for (int j = i + 1; j < lSize; j++) {
                repel(x, l.get(j), speed, maxRepelDist);
            }
        }
    }

    private static void attract(Collidable x, Collidable y, float speed, float idealDistRads) {
        SimpleSpatial xp = ((SimpleSpatial) x.data());
        SimpleSpatial yp = ((SimpleSpatial) y.data());

        v3 delta = v();
        delta.sub(yp.transform(), xp.transform());


        float len = delta.normalize();
        //len -= idealDistRads * Math.max(xp.radius(), yp.radius());
        //if (len <= 0)
            //return;


        //v3 delta2 = v(delta);

        delta.scale((speed * (xp.mass() /* + yp.mass()*/) ) * len );
        ((Dynamic) x).impulse(delta);
//        delta2.scale(-(speed * (yp.mass() /* + yp.mass()*/) ) * len  );
//        ((Dynamic) y).impulse(delta2);

    }

    private static void repel(Collidable x, Collidable y, float speed, float maxDist) {
        SimpleSpatial xp = ((SimpleSpatial) x.data());
        SimpleSpatial yp = ((SimpleSpatial) y.data());

        v3 delta = v();
        delta.sub(xp.transform(), yp.transform());

        float len = delta.normalize();

        //len -= (xp.radius() + yp.radius());

        if (len >= maxDist)
            return;
        else {
            len = Math.max(0, len);
        }

        float base = speed / ( Util.sqr( 1 + len ));

        v3 yx = v(delta);
        yx.scale(xp.mass() * base );
        ((Dynamic) x).impulse(yx);

        v3 xy = v(delta);
        xy.scale( -yp.mass() * base );
        ((Dynamic) y).impulse(xy);

    }


}
