package spacegraph;

import com.jogamp.opengl.GL2;
import spacegraph.phys.Collidable;
import spacegraph.phys.shape.BoxShape;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.render.Draw;

/**
 * Created by me on 9/13/16.
 */
public abstract class AbstractSpatial<X> extends Spatial<X> {

    public AbstractSpatial(X x) {
        super(x);
    }

    public void renderAbsolute(GL2 gl) {
        //blank
    }

    public void renderRelative(GL2 gl, Collidable body) {

        colorshape(gl);
        Draw.draw(gl, body.shape());

        CollisionShape shape = body.shape();

        if (shape instanceof BoxShape) {
            //render surface on BoxShape face

            BoxShape bshape = (BoxShape) shape;

            gl.glPushMatrix();

            float sx = bshape.x(); //HACK
            float sy = bshape.y(); //HACK

            //if (sx > sy) {
            float ty = sy;
            float tx = sy / sx;
            //} else {
            //  tx = sx;
            //  ty = sx/sy;
            //}

            //gl.glTranslatef(-1/4f, -1/4f, 0f); //align TODO not quite right yet

            gl.glScalef(tx, ty, 1f);

            renderRelativeAspect(gl);

            gl.glPopMatrix();
        }

    }

    protected void renderRelativeAspect(GL2 gl) {

    }

    protected void colorshape(GL2 gl) {
        gl.glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
    }


}
