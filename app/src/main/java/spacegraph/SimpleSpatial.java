package spacegraph;

import com.jogamp.opengl.GL2;
import nars.$;
import nars.util.Util;
import spacegraph.math.Quat4f;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.Dynamics;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.phys.math.Transform;
import spacegraph.phys.shape.BoxShape;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.util.Motion;
import spacegraph.render.Draw;

import java.util.Collections;
import java.util.List;

/** simplified implementation which manages one body and N constraints. useful for simple objects */
public class SimpleSpatial<X> extends Spatial<X> {



    /** physics motion state */
    public final Motion motion = new Motion();
    private final String label;
    private final CollisionShape shape;

    /** prevents physics movement */
    public boolean motionLock;

    public float radius = 0;
    private List<Collidable<X>> bodies = Collections.emptyList();

    protected float[] shapeColor;

    public SimpleSpatial(X x) {
        super(x);

        shapeColor = new float[] { 0.5f, 0.5f, 0.5f, 0.9f };
        this.shape = newShape();

        this.label = key!=null ? key.toString() : super.toString();

    }

    public Dynamic body;
    private final List<TypedConstraint> constraints = $.newArrayList(0);


    public final Transform transform() {
        Dynamic b = this.body;
        return b == null ? motion.t : b.transform();
    }

    public void moveX(float x, float rate) {
        v3 center = transform();
        move(Util.lerp(x, center.x, rate), center.y, center.z);
    }
    //TODO moveY
    public void moveZ(float z, float rate) {
        v3 center = transform();
        move(center.x, center.y, Util.lerp(z, center.z, rate));
    }

    public void move(float x, float y, float z, float rate) {
        v3 center = transform();
        move(
                Util.lerp(x, center.x, rate),
                Util.lerp(y, center.y, rate),
                Util.lerp(z, center.z, rate)
        );
    }

    public final void move(v3 p) {
        move(p.x, p.y, p.z);
    }

    public void move(float x, float y, float z) {
        if (motionLock)
            return;

        transform().set(x,y,z);
        reactivate();
    }

    /** interpolates rotation to the specified axis vector and rotation angle around it */
    public void rotate(float nx, float ny, float nz, float angle, float speed) {
        if (motionLock)
            return;

        Quat4f tmp = new Quat4f();

        Quat4f target = new Quat4f();
        target.setAngle(nx,ny,nz,angle);

        rotate(target, speed, tmp);
    }


    public void rotate(Quat4f target, float speed, Quat4f tmp) {
        if (motionLock)
            return;

        Quat4f current = transform().getRotation(tmp);
        current.interpolate(target, speed);
        transform().setRotation(current);
    }


    public void reactivate() {
        Dynamic b = body;
        if (b !=null/* && !b.isActive()*/)
            b.activate(collidable());
    }

    @Override
    public void update(SpaceGraph<X> s) {
        super.update(s);
        if (body == null) {
            enter(s);
        } else {
            body.activate();
        }

    }

    public void moveDelta(v3 v, float speed) {
        moveDelta(v.x, v.y, v.z, speed);
    }

    public void moveDelta(float dx, float dy, float dz, float speed) {
        move(
                x() + dx,
                y() + dy,
                z() + dz,
                speed);
    }
    public void moveDelta(float dx, float dy, float dz) {
        move(
                x() + dx,
                y() + dy,
                z() + dz);
    }

    public void scale(float sx, float sy, float sz) {

        ((BoxShape)shape).size(sx, sy, sz);
        this.radius = Math.max(sx, Math.max(sy, sz));

    }

    //TODO make abstract
    protected CollisionShape newShape() {
        return new BoxShape(v3.v(1, 1, 1));
    }

    public Dynamic newBody(boolean collidesWithOthersLikeThis) {
        Dynamic b;
        b = Dynamics.newBody(
                1f, //mass
                shape, motion,
                +1, //group
                collidesWithOthersLikeThis ? -1 : -1 & ~(+1) //exclude collisions with self
        );

        //b.setLinearFactor(1,1,0); //restricts movement to a 2D plane


        b.setDamping(0.9f, 0.5f);
        b.setFriction(0.9f);

        return b;
    }


    protected void renderAbsolute(GL2 gl) {
        //blank
    }

    @Override public final void accept(GL2 gl, Dynamic body) {

        renderAbsolute(gl);

        gl.glPushMatrix();

        Draw.transform(gl, body.transform());

        renderRelative(gl, body);

        gl.glPushMatrix();
        BoxShape shape = (BoxShape) body.shape();
        float sx = shape.x(); //HACK
        float sy = shape.y(); //HACK
        float tx, ty;
        //if (sx > sy) {
        ty = sy;
        tx = sy/sx;
        //} else {
        //  tx = sx;
        //  ty = sx/sy;
        //}

        //gl.glTranslatef(-1/4f, -1/4f, 0f); //align TODO not quite right yet

        gl.glScalef(tx, ty, 1f);

        renderRelativeAspect(gl);
        gl.glPopMatrix();

        gl.glPopMatrix();
    }

    protected void renderRelative(GL2 gl, Dynamic body) {

        renderShape(gl, body);

    }

    protected void renderRelativeAspect(GL2 gl) {

    }

    protected void renderLabel(GL2 gl, float scale) {
        final float charAspect = 1.5f;
        Draw.renderLabel(gl, scale, scale / charAspect, label, 0, 0, 0.5f);
    }

    protected void renderShape(GL2 gl, Dynamic body) {
        colorshape(gl);
        Draw.draw(gl, body.shape());
    }

    protected void colorshape(GL2 gl) {
        gl.glColor4fv(shapeColor, 0);
    }


//    @Override
//    public void start(short order) {
//        super.start(order);
//        reactivate();
//    }

    public void motionLock(boolean b) {
        motionLock = b;
    }
    public float x() {  return transform().x;        }
    public float y() {  return transform().y;        }
    public float z() {  return transform().z;        }

//    protected void updateContinue() {
//        //if (body.broadphase()==null)
//        //reactivate();
//
//    }

    @Override
    public final void update(Dynamics world) {
        if (body == null) {
            this.bodies = enter(world);
        } else {
            next(world);
        }
    }

    protected void next(Dynamics world) {

    }

    protected List<Collidable<X>> enter(Dynamics world) {
        Dynamic b = body = newBody(collidable());
        b.setUserPointer(this);
        b.setRenderer(this);
        return Collections.singletonList(body);
    }

    protected void enter(SpaceGraph<X> s) {

    }

    @Override
    public void stop(Dynamics s) {
        super.stop(s);
        body = null;
    }


    @Override
    public List<Collidable<X>> bodies() {
        return bodies;
    }

    @Override
    public List<TypedConstraint> constraints() {
        return constraints;
    }


}
