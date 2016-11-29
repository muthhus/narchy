package spacegraph.obj;

import com.jogamp.opengl.GL2;
import nars.util.Util;
import org.jetbrains.annotations.Nullable;
import spacegraph.SimpleSpatial;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.phys.math.Transform;
import spacegraph.phys.shape.BoxShape;
import spacegraph.phys.shape.SimpleBoxShape;
import spacegraph.render.Draw;

import static spacegraph.math.v3.v;

/**
 * https://en.wikipedia.org/wiki/Cuboid
 * Serves as a mount for an attached (forward-facing) 2D surface (embeds a surface in 3D space)
 */
public class Cuboid<X> extends SimpleSpatial<X> {

    @Nullable
    public Surface front;
    final float zOffset = 0.1f; //relative to scale

    @Nullable public Finger mouseFront;
    private v3 mousePick;
    //private float padding;

    public Cuboid(X x, float w, float h) {
        this(x, null, w, h);
    }

    public Cuboid(Surface front, float w, float h) {
        this((X) front, front, w, h);
    }

    public Cuboid(X x, Surface front, float w, float h) {
        this(x, front, w, h, (Math.min(w, h)/2f));
    }

    public Cuboid(Surface front, float w, float h, float d) {
        this((X)front, front, w, h, d);
    }

    public Cuboid(X x, Surface front, float w, float h, float d) {
        super(x);

        scale(w, h, d);



        setFront(front);

    }

    public void setFront(Surface front) {
        this.front = front;
        if (front!=null) {
            front.setParent(null);
            mouseFront = new Finger(front);
        } else {
            mouseFront = null;
        }
    }

    @Override
    public boolean onKey(Collidable body, v3 hitPoint, char charCode, boolean pressed) {
        if (!super.onKey(body, hitPoint, charCode, pressed)) {

            return front != null && front.onKey(null, charCode, pressed);
        }
        return true;
    }

    @Override
    public Surface onTouch(Collidable body, ClosestRay r, short[] buttons) {

        Surface s0 = super.onTouch(body, r, buttons);
        if (s0 != null)
            return s0;

        if (front!=null) {
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

                this.mousePick = r.hitPointWorld;

                //System.out.println(localPoint + " " + thick);
                return mouseFront.on(new v2(localPoint.x / shape.x() + 0.5f, localPoint.y / shape.y() + 0.5f), buttons);
            } else {
            }
        }

        this.mousePick = null;

        return null;
    }


    @Override
    public final void renderRelative(GL2 gl, Collidable body) {
        super.renderRelative(gl, body);


        if (front!=null) {

            //float p = this.padding;

            //gl.glPushMatrix();

            //float pp = 1f - (p / 2f);
            //float pp = 1f;

            gl.glTranslatef(-0.5f, -0.5f, 0.5f + zOffset);
            //gl.glScalef(pp, pp, 1f);

            //Transform t = transform();
            //float tw = t.x;
            //float th = t.y;
            gl.glDepthMask(false);
            front.render(gl, v(1, 1));
            gl.glDepthMask(true);

            //gl.glPopMatrix();
        }
    }

    @Override
    public void renderAbsolute(GL2 gl) {
        super.renderAbsolute(gl);

        //display pick location (debugging)
        if (mousePick!=null) {
            gl.glPushMatrix();
            gl.glTranslatef(mousePick.x, mousePick.y, mousePick.z);
            gl.glScalef(0.25f, 0.25f, 0.25f);
            gl.glColor4f(1f, 1f, 1f, 0.5f);
            gl.glRotated(Math.random()*360.0, Math.random()-0.5f, Math.random()-0.5f, Math.random()-0.5f);
            gl.glDepthMask(false);
            Draw.rect(gl, -0.5f, -0.5f, 1, 1);
            gl.glDepthMask(true);
            gl.glPopMatrix();
        }
    }
}
