package spacegraph.layout;

import jcog.data.FloatParam;
import spacegraph.SimpleSpatial;
import spacegraph.Spatial;
import spacegraph.math.v3;
import spacegraph.phys.BulletGlobals;
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

    boolean center = true;

    public final FloatParam repel = new FloatParam(2.5f, 0, 5f);
    public final FloatParam attraction = new FloatParam(0.05f, 0, 2f);



    /** speed at which center correction is applied */
    float centerSpeed = 0.25f;

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
        maxRepelDist = r*0.75f;
    }


    @Override
    public void solve(Broadphase b, List<Collidable> objects, float timeStep) {

        int n = objects.size();
        if (n == 0)
            return;

        objects.forEach(c -> ((Spatial)c.data()).stabilize(boundsMin, boundsMax));

        //System.out.print("Force direct " + objects.size() + ": ");
        //final int[] count = {0};
        //count[0] += l.size();
//System.out.print(l.size() + "  ");
        b.forEach((int) Math.ceil((float) n / clusters), objects, this::batch);
        //System.out.println(" total=" + count[0]);


        if (center) {
            float cx = 0, cy = 0, cz = 0;
            for (int i = 0, objectsSize = n; i < objectsSize; i++) {
                v3 c = objects.get(i).worldTransform;
                cx += c.x;
                cy += c.y;
                cz += c.z;
            }
            cx /= -n;
            cy /= -n;
            cz /= -n;

            v3 correction = v3.v(cx, cy, cz);
            if (correction.lengthSquared() > centerSpeed*centerSpeed)
                correction.normalize(centerSpeed);

            for (int i = 0, objectsSize = n; i < objectsSize; i++) {
                objects.get(i).worldTransform.add(correction);
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

    protected static void attract(Collidable x, Collidable y, float speed, float idealDistRads) {
        SimpleSpatial xp = ((SimpleSpatial) x.data());
        SimpleSpatial yp = ((SimpleSpatial) y.data());

        v3 delta = v();
        delta.sub(yp.transform(), xp.transform());

        float len = delta.normalize();
        if (!Float.isFinite(len))
            return;

        len -= idealDistRads;
        if (len < 0) {
            len*=-1;
            speed*=-1;
        }
        if (len < BulletGlobals.FLT_EPSILON)
            return;


        //v3 delta2 = v(delta);

        //delta.scale((speed / (1 + /&xp.mass() /* + yp.mass()*/) ) * len );
        delta.scale( (float)(len) * speed );
        ((Dynamic) x).impulse(delta);
        //delta2.scale(-(speed * (yp.mass() /* + yp.mass()*/) ) * len  );
        delta.scale(-1 );
        ((Dynamic) y).impulse(delta);

    }

    private static void repel(Collidable x, Collidable y, float speed, float maxDist) {
        SimpleSpatial xp = ((SimpleSpatial) x.data());
        SimpleSpatial yp = ((SimpleSpatial) y.data());

        v3 delta = v();
        delta.sub(xp.transform(), yp.transform());

        float len = delta.normalize();
        len -= (xp.radius() + yp.radius());
        if (len < 0)
            len = 0;
        else if (len >= maxDist)
            return;

        float base = speed / ( 1 + ( len ));

        float mr = yp.mass() / xp.mass();

        {
            float s = mr * base;
            ((Dynamic) x).impulse(delta.x * s, delta.y * s, delta.z * s);
        }

        {
            float s = -(1f/mr) * base;
            ((Dynamic) y).impulse(delta.x * s, delta.y * s, delta.z * s);
        }

    }


}
