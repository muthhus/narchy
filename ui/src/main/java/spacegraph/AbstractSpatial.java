package spacegraph;

import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.phys.Collidable;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.shape.SimpleBoxShape;
import spacegraph.render.Draw;

import java.util.Collections;
import java.util.List;

/**
 * Created by me on 9/13/16.
 */
public abstract class AbstractSpatial<X> extends Spatial<X> {

    public AbstractSpatial(X x) {
        super(x);
    }

    @Nullable
    @Override
    public List<TypedConstraint> constraints() {
        return Collections.emptyList();
    }

    @Override
    public void renderAbsolute(GL2 gl) {
        //blank
    }

    @Override
    public void renderRelative(GL2 gl, Collidable body) {

        colorshape(gl);
        Draw.draw(gl, body.shape());

        CollisionShape shape = body.shape();

        if (shape instanceof SimpleBoxShape) {
            //render surface on BoxShape face

            SimpleBoxShape bshape = (SimpleBoxShape) shape;

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
