package nars.gui.graph;

import bulletphys.dynamics.RigidBody;
import com.jogamp.opengl.GL2;

/**
 * A mount for a 2D surface (embeds a surface in 3D space)
 */
public class SurfaceMount<X> extends Atomatter<X> {

    public final Surface surface;

    public SurfaceMount(X x, Surface s) {
        super(x);

        this.surface = s;

        //scale(4f,3f,0.1f);
        radius = 0.5f; //HACK

        s.setParent(null);
    }



    @Override
    protected final void renderRelative(GL2 gl, RigidBody body) {
        super.renderRelative(gl, body);

        gl.glPushMatrix();
        gl.glTranslatef(0, 0, radius*1.05f);
        surface.render(gl);
        gl.glPopMatrix();
    }

}
