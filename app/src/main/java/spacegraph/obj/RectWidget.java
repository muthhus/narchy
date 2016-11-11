package spacegraph.obj;

import com.jogamp.opengl.GL2;
import nars.util.Util;
import spacegraph.SimpleSpatial;
import spacegraph.Surface;
import spacegraph.math.v2;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.phys.math.Transform;
import spacegraph.phys.shape.BoxShape;

import static spacegraph.math.v3.v;
import static spacegraph.math.v3.v;

/**
 * A mount for a 2D surface (embeds a surface in 3D space)
 */
public class RectWidget<X> extends SimpleSpatial<X> {

    public final Surface surface;
    final float zOffset = 0.05f; //relative to scale

    //private float padding;


    public RectWidget(Surface s, float w, float h) {
        this((X) s, s, w, h);
    }

    public RectWidget(X x, Surface s, float w, float h) {
        super(x);

        this.surface = s;

        final float thick = 0.2f;
        scale(w, h, thick * (Math.min(w, h)));

        s.setParent(null);
    }

    @Override
    public boolean onKey(Collidable body, v3 hitPoint, char charCode, boolean pressed) {
        if (!super.onKey(body, hitPoint, charCode, pressed)) {

            return surface != null && surface.onKey(null, charCode, pressed);
        }
        return true;
    }

    @Override
    public Surface onTouch(Collidable body, ClosestRay r, short[] buttons) {
        Surface s0 = super.onTouch(body, r, buttons);

        if (s0 != null)
            return s0;

        Transform it = Transform.t(transform()).inverse();
        v3 localPoint = it.transform(v(r.hitPointWorld));


//            //TODO maybe do this test with the normal vector of the hit ray
//            if (this.thick!=this.thick) {
//                Vector3f h = ((BoxShape) body.shape()).getHalfExtentsWithMargin(v());
//                this.thick = h.z;
//            }

        BoxShape shape = (BoxShape) body.shape();
        float frontZ = shape.z() / 2;
        float zTolerance = frontZ / 4f;

        if (Util.equals(localPoint.z, frontZ, zTolerance)) { //top surface only, ignore sides and back

            //System.out.println(localPoint + " " + thick);
            return surface.onTouch(new v2(localPoint.x / shape.x() + 0.5f, localPoint.y / shape.y() + 0.5f), buttons);
        }


        return null;
    }


    @Override
    protected final void renderRelative(GL2 gl, Dynamic body) {
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
