package nars.gui.graph;

import bulletphys.collision.shapes.BoxShape;
import bulletphys.collision.shapes.CollisionShape;
import bulletphys.dynamics.RigidBody;
import com.jogamp.opengl.GL2;

import javax.vecmath.Vector3f;

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
    protected CollisionShape newShape() {
        return new BoxShape(Vector3f.v(1, 1, 0.05f));
    }

    @Override
    protected final void renderRelative(GL2 gl, RigidBody body) {
        super.renderRelative(gl, body);

        float p = this.padding;
        gl.glPushMatrix();
        float pp = 1f - (p / 2f);

        gl.glTranslatef(-radius * pp, -radius * pp, radius*1.05f);
        gl.glScalef(pp, pp, 1f);

        surface.render(gl);
        gl.glPopMatrix();
    }

}
