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
import java.util.function.Consumer;

import static spacegraph.math.v3.v;

/** simplified implementation which manages one body and N constraints. useful for simple objects */
public class SimpleSpatial<X> extends AbstractSpatial<X> {



    /** physics motion state */
    public final Motion motion = new Motion();
    private final String label;
    protected CollisionShape shape;

    /** prevents physics movement */
    public boolean motionLock;

    public float radius;
    private List<Collidable> bodies = Collections.emptyList();

    public float[] shapeColor;

    public SimpleSpatial(X x) {
        super(x);

        shapeColor = new float[] { 0.5f, 0.5f, 0.5f, 0.9f };
        this.shape = newShape();

        this.label = label(x);

    }

    protected String label(X x) {
        return x!=null ? x.toString() : super.toString();
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
            next(s);
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

        if (shape instanceof BoxShape)
            ((BoxShape)shape).size(sx, sy, sz);
        else
            shape.setLocalScaling(v(sx,sy,sz));

        this.radius = Math.max(sx, Math.max(sy, sz));

    }



    //TODO make abstract
    protected CollisionShape newShape() {
        return new BoxShape(v(1, 1, 1));
    }

    public Dynamic newBody(boolean collidesWithOthersLikeThis) {
        Dynamic b;
        b = Dynamics.newBody(
                mass(), //mass
                shape, motion,
                +1, //group
                collidesWithOthersLikeThis ? -1 : -1 & ~(+1) //exclude collisions with self
        );

        //b.setLinearFactor(1,1,0); //restricts movement to a 2D plane


        b.setDamping(0.9f, 0.5f);
        b.setFriction(0.9f);

        return b;
    }

    public float mass() {
        return 1f;
    }


    @Override protected void colorshape(GL2 gl) {
        gl.glColor4fv(shapeColor, 0);
    }

    protected void renderLabel(GL2 gl, float scale) {
        final float charAspect = 1.5f;
        Draw.text(gl, scale, scale / charAspect, label, 0, 0, 0.5f);
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
    public void update(Dynamics world) {
        if (bodies.isEmpty()) {
            this.bodies = Collections.singletonList( create(world) );
        } else {
            next(world);
        }
    }

    protected void next(Dynamics world) {

    }

    protected Dynamic create(Dynamics world) {
        Dynamic b = body = newBody(collidable());
        b.setData(this);
        b.setRenderer(this);
        return b;
    }

    protected void next(SpaceGraph<X> s) {

    }

    @Override
    public void stop(Dynamics s) {
        super.stop(s);
        body = null;
    }

    @Override
    public List<TypedConstraint> constraints() {
        return constraints;
    }


    @Override
    public void forEachBody(Consumer<Collidable> c) {
        bodies.forEach(c);
    }
}
