package nars.experiment.rover;

import com.jogamp.opengl.GL2;
import nars.$;
import nars.NAR;
import nars.nar.Default;
import spacegraph.SimpleSpatial;
import spacegraph.SpaceGraph;
import spacegraph.math.v3;
import spacegraph.obj.Maze;
import spacegraph.phys.Collisions;
import spacegraph.phys.Dynamics;
import spacegraph.phys.math.Transform;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.shape.CylinderShape;
import spacegraph.render.Draw;

import java.util.List;

import static spacegraph.math.v3.v;

/**
 * Created by me on 9/12/16.
 */
public class RoverMaze1 {

    public static class Retina extends Collisions.RayResultCallback {
        public v3 localPosition, worldPosition;
        public v3 localDirection, worldTarget, worldHit = v();
        float r, g, b;
        public float rangeMax;
        private SimpleSpatial parent;

        public void update(Dynamics d, SimpleSpatial parent) {
            this.parent = parent;
            Transform x = parent.transform();

            worldPosition = x.transform(v(localPosition));

            worldTarget = v(localDirection);
            worldTarget.scale(rangeMax); //TODO limit by contact point
            worldTarget.add(localPosition);
            x.transform(worldTarget);


            r = g = b = 0;
            worldHit.set(worldTarget);

            d.rayTest(worldPosition, worldTarget, this);
        }

        public void render(GL2 gl) {

            gl.glColor3f(r, g, b);
            gl.glLineWidth(2f);
            Draw.line(gl, worldPosition, worldTarget);
        }

        @Override
        public float addSingleResult(Collisions.LocalRayResult rayResult, boolean normalInWorldSpace) {
            Object target = rayResult.collidable.data();
            if (target != parent) {
                float dist = v3.dist(worldPosition, rayResult.hitNormal);
                //System.out.println(rayResult.collidable.data() + " " + dist);
                worldHit.set(rayResult.hitNormal);
                if (target instanceof SimpleSpatial) {
                    SimpleSpatial ss = ((SimpleSpatial) target);
                    r = ss.shapeColor[0];
                    g = ss.shapeColor[1];
                    b = ss.shapeColor[2];
                }
            }
            return 0;
        }
    }

    public static class Rover extends SimpleSpatial {

        private final NAR nar;

        final List<Retina> retinas = $.newArrayList();

        public Rover(NAR nar) {
            super(nar);
            this.nar = nar;

            shapeColor[0] = 1f;
            shapeColor[1] = 0.1f;
            shapeColor[2] = 0.5f;
            shapeColor[3] = 1f;

        }

        public void addRetinaGrid(v3 src, v3 fwd, v3 left, v3 up, int w, int h, float rangeMax) {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    Retina r = new Retina();

                    r.localPosition = src; ///v(src);

                    r.localDirection = v(fwd);
                    r.localDirection.addScaled(left, 2f * (((float)x)/(w-1) - 0.5f));
                    r.localDirection.addScaled(up, 2f * (((float)y)/(h-1) - 0.5f));

                    r.rangeMax = rangeMax;

                    retinas.add(r);
                }
            }

        }

        @Override
        public void update(Dynamics world) {
            for (Retina r : retinas)
                r.update(world, this);

            super.update(world);
        }

        @Override
        protected void renderAbsolute(GL2 gl) {
            for (Retina r : retinas)
                r.render(gl);

            super.renderAbsolute(gl);
        }

        @Override
        protected CollisionShape newShape() {
            //return new TetrahedronShapeEx(v(0,10,0), v(10,0,0), v(10,10,0), v(0,0,10));
            return new CylinderShape(v(1, 1, 1));
        }
    }

    public static void main(String[] args) {
        Rover r = new Rover(new Default());
        r.addRetinaGrid(v(), v(0,0,1), v(0.1f,0,0), v(0,0.1f,0), 6,6, 4f);

        new SpaceGraph<>(
                new Maze("x", 20, 20),
                r
        ).setGravity(v(0, 0, -5)).show(1000, 1000);
    }

}
