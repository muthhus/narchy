package spacegraph.obj;

import com.jogamp.opengl.GL2;
import nars.util.Util;
import spacegraph.Spatial;
import spacegraph.Surface;
import spacegraph.phys.collision.dispatch.ClosestRay;
import spacegraph.phys.collision.shapes.BoxShape;
import spacegraph.phys.collision.shapes.CollisionShape;
import spacegraph.phys.dynamics.RigidBody;
import spacegraph.phys.linearmath.Transform;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import static javax.vecmath.Vector3f.v;

/**
 * A mount for a 2D surface (embeds a surface in 3D space)
 */
public class RectWidget<X> extends Spatial<X> {

    public final Surface surface;
    private BoxShape shape;
    final float zOffset = 0.05f; //relative to scale

    //private float padding;


    public RectWidget(Surface s, float w, float h) {
        this((X)s,s, w, h);
    }

    public RectWidget(X x, Surface s, float w, float h) {
        super(x);

        this.surface = s;

        final float thick = 0.2f;
        this.shape = new BoxShape(w, h, thick * (Math.min(w,h)));

        s.setParent(null);
    }



    @Override
    public boolean onTouch(ClosestRay r, short[] buttons) {
        if (!super.onTouch(r, buttons)) {


            Transform it = Transform.t(transform()).inverse();
            Vector3f localPoint = it.transform(v(r.hitPointWorld));


//            //TODO maybe do this test with the normal vector of the hit ray
//            if (this.thick!=this.thick) {
//                Vector3f h = ((BoxShape) body.shape()).getHalfExtentsWithMargin(v());
//                this.thick = h.z;
//            }

            BoxShape shape = (BoxShape) body.shape();
            float frontZ = shape.z()/2;
            float zTolerance = frontZ/4f;

            if (Util.equals(localPoint.z, frontZ, zTolerance)) { //top surface only, ignore sides and back

                //System.out.println(localPoint + " " + thick);
                return surface.onTouch(new Vector2f(localPoint.x / shape.x() + 0.5f, localPoint.y / shape.y() + 0.5f), buttons);
            }
        }
        return false;
    }

    @Override
    protected CollisionShape newShape() {
        return shape;
    }




    @Override
    protected final void renderRelative(GL2 gl, RigidBody body) {
        super.renderRelative(gl, body);

        //float p = this.padding;

        gl.glPushMatrix();

        //float pp = 1f - (p / 2f);
        //float pp = 1f;

        gl.glTranslatef(-0.5f, -0.5f, 0.5f + zOffset);
        //gl.glScalef(pp, pp, 1f);


        surface.render(gl);

        gl.glPopMatrix();
    }

}
