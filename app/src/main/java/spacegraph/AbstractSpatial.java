package spacegraph;

import com.jogamp.opengl.GL2;
import spacegraph.phys.Dynamic;
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

    protected void renderAbsolute(GL2 gl) {
        //blank
    }

    @Override
    public final void accept(GL2 gl, Dynamic body) {

        renderAbsolute(gl);

        gl.glPushMatrix();

        Draw.transform(gl, body.transform());

        renderRelative(gl, body);

        gl.glPopMatrix();

        gl.glPopMatrix();
    }

    protected void renderRelative(GL2 gl, Dynamic body) {

        renderShape(gl, body);

    }

    protected void renderRelativeAspect(GL2 gl) {

    }

    protected void colorshape(GL2 gl) {
        gl.glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
    }


    protected void renderShape(GL2 gl, Dynamic body) {
        colorshape(gl);
        Draw.draw(gl, body.shape());

        CollisionShape shape = body.shape();

        if (shape instanceof BoxShape) {
            //render surface on BoxShape face

            BoxShape bshape = (BoxShape) shape;

            gl.glPushMatrix();
            float sx, sy;
            sx = bshape.x(); //HACK
            sy = bshape.y(); //HACK
            float tx, ty;
            //if (sx > sy) {
            ty = sy;
            tx = sy / sx;
            //} else {
            //  tx = sx;
            //  ty = sx/sy;
            //}

            //gl.glTranslatef(-1/4f, -1/4f, 0f); //align TODO not quite right yet

            gl.glScalef(tx, ty, 1f);

            renderRelativeAspect(gl);
        }

    }

}
