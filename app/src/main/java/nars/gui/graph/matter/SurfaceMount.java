package nars.gui.graph.matter;

import bulletphys.collision.shapes.BoxShape;
import bulletphys.collision.shapes.CollisionShape;
import bulletphys.dynamics.RigidBody;
import bulletphys.linearmath.Transform;
import com.jogamp.opengl.GL2;
import nars.gui.graph.Atomatter;
import nars.gui.graph.Surface;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import static bulletphys.linearmath.Transform.t;
import static javax.vecmath.Vector3f.v;

/**
 * A mount for a 2D surface (embeds a surface in 3D space)
 */
public class SurfaceMount<X> extends Atomatter<X> {

    public final Surface surface;
    private float padding;

    public SurfaceMount(X x, Surface s) {
        super(x);

        this.surface = s;


        //scale(4f,3f,0.1f);
        this.radius = 0.5f; //HACK

        this.padding = 0.04f;

        s.setParent(null);
    }

    @Override
    public boolean onTouch(Vector3f hitPoint, short[] buttons) {
        if (!super.onTouch(hitPoint, buttons)) {
            Transform it = Transform.t(transform()).inverse();
            Vector3f localPoint = it.transform(v(hitPoint));
            return surface.onTouch(new Vector2f(localPoint.x/2f+0.5f, localPoint.y/2f+0.5f), buttons);
        }
        return false;
    }

    @Override
    protected CollisionShape newShape() {
        return new BoxShape(v(1, 1, 0.05f));
    }

    @Override
    protected final void renderRelative(GL2 gl, RigidBody body) {
        super.renderRelative(gl, body);

        //float p = this.padding;

        gl.glPushMatrix();

        //float pp = 1f - (p / 2f);
        float pp = 1f;

        gl.glTranslatef(-0.5f, -0.5f, radius*1.05f);
        //gl.glScalef(pp, pp, 1f);

        surface.render(gl);
        gl.glPopMatrix();
    }

}
