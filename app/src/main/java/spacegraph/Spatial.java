package spacegraph;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.Quaternion;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;
import spacegraph.phys.collision.shapes.BoxShape;
import spacegraph.phys.collision.shapes.CollisionShape;
import spacegraph.phys.collision.shapes.ConvexInternalShape;
import spacegraph.phys.dynamics.RigidBody;
import spacegraph.phys.linearmath.Transform;
import spacegraph.phys.util.Motion;
import spacegraph.render.ShapeDrawer;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import java.util.function.BiConsumer;

/**
 * volumetric subspace.
 * an atom (base unit) of spacegraph physics-simulated virtual matter
 */
public class Spatial<O> implements BiConsumer<GL2, RigidBody> {


    public final O key;
    public final int hash;

    @NotNull
    public final EDraw[] edges;



    public RigidBody body;

    /** cached center reference */
    public transient final Vector3f center; //references a field in MotionState's transform

    /** physics motion state */
    public final Motion motion = new Motion();

    /** prevents physics movement */
    public boolean motionLock;



    /** cached radius */
    transient public float radius;

    /** cached .toString() of the key */
    public String label;

    public float pri;

    /**
     * the draw order if being drawn
     * order = -1: inactive
     * order > =0: live
     */
    transient public short order;


//    //TODO
//    public boolean physical() {
//        return true;
//    }



    public Spatial() {
        this(null);
    }

    public Spatial(O k) {
        this(k, 0);
    }

    @Deprecated public Spatial(O k, int edges) {
        this.key = k!=null ? k : (O) this;
        this.label = key!=null ? key.toString() : super.toString();
        this.hash = k!=null ? k.hashCode() : super.hashCode();
        this.edges = new EDraw[edges];
        this.radius = 0;
        this.pri = 0.5f;

        final float initDistanceEpsilon = 0.5f;
        move(SpaceGraph.r(initDistanceEpsilon),
             SpaceGraph.r(initDistanceEpsilon),
             SpaceGraph.r(initDistanceEpsilon));

        for (int i = 0; i < edges; i++)
            this.edges[i] = new EDraw();

        //init physics
        center = motion.t.origin;

    }

    @Override
    public String toString() {

        return label + "<" + body + " " +
                //(body!=null ? body.shape() : "shapeless")  +
                " " + (active() ? "on" : "off")+

                ">";
    }

    public final Transform transform() { return motion.t; }


    @Override
    public final boolean equals(Object obj) {
        return this == obj || key.equals(((Spatial) obj).key);
    }

    @Override
    public final int hashCode() {
        return hash;
    }


    @Deprecated public transient int numEdges = 0;


    public void start(SpaceGraph s) {
        preactive = true;

        if (body == null) {
            RigidBody b = body = newBody(s, newShape(), collidable());
            b.setUserPointer(this);
            b.setRenderer(this);
        } else {
            if (body.broadphase()==null)
                throw new NullPointerException();
            reactivate();
        }
    }

    public void clearEdges() {
        this.numEdges = 0;
    }

    public boolean active() {
        return preactive || order > -1;
    }

    public boolean preactive;

    public final void unpreactivate() {
        preactivate(false);
    }
    public final void preactivate(boolean b) {
        this.preactive = b;
    }

    public final void inactivate() {
        this.order = -1;
    }

    public void move(float x, float y, float z, float rate) {
        move(
                Util.lerp(x, center.x, rate),
                Util.lerp(y, center.y, rate),
                Util.lerp(z, center.z, rate)
        );
    }
    public void move(float x, float y, float z) {
        if (!motionLock) {

            RigidBody b = this.body;
            if (b !=null) {
                b.transform().origin.set(x,y,z);

//                    com.Transform t = new com.Transform();
//                    body.getCenterOfMassTransform(t);
//                    t.origin.set(x, y, z);
//                    body.setCenterOfMassTransform(t);

                reactivate();
            } else {
                motion.t.origin.set(x, y, z);
            }
        }

//            float[] p = this.p;
//            p[0] = x;
//            p[1] = y;
//            p[2] = z;
    }

    public void reactivate() {
        RigidBody b = body;
        if (b !=null/* && !b.isActive()*/)
            b.activate(collidable());
    }

    public int edgeCount() {
        return numEdges;
    }

    public float x() {  return center.x;        }
    public float y() {  return center.y;        }
    public float z() {  return center.z;        }



    public void moveDelta(float dx, float dy, float dz) {
        move(
            x() + dx,
            y() + dy,
            z() + dz);
    }

    public void scale(float sx, float sy, float sz) {

        if (body!=null) {
            ((BoxShape) this.body.shape()).size(sx, sy, sz);
            this.radius = Math.max(sx, Math.max(sy, sz));
        } else {
            this.radius = 0;
        }

    }

    public void motionLock(boolean b) {
        motionLock = b;
    }

    /** called on registration into the physics engine list of objects */
    public void activate(short order) {

        this.order = order;
        reactivate();

    }

    public boolean collidable() {
        return true;
    }

    /** returns true if the event has been absorbed, false if it should continue propagating */
    public boolean onTouch(Vector3f hitPoint, short[] buttons) {
        return false;
    }

    /** returns true if the event has been absorbed, false if it should continue propagating */
    public boolean onKey(Vector3f hitPoint, char charCode) {
        return false;
    }


    //TODO make abstract
    protected CollisionShape newShape() {
        return new BoxShape(Vector3f.v(1, 1, 1));
    }



    public RigidBody newBody(SpaceGraph graphSpace, CollisionShape shape, boolean collidesWithOthersLikeThis) {
        RigidBody b;
        b = graphSpace.newBody(
                1f, //mass
                shape, motion,
                +1, //group
                collidesWithOthersLikeThis ? -1 : -1 & ~(+1) //exclude collisions with self
        );

        //b.setLinearFactor(1,1,0); //restricts movement to a 2D plane


        b.setDamping(0.99f, 0.5f);
        b.setFriction(0.9f);

        return b;
    }


    @Override public final void accept(GL2 gl, RigidBody body) {

        renderAbsolute(gl);

        gl.glPushMatrix();
        ShapeDrawer.transform(gl, body.transform());
        renderRelative(gl, body);

        gl.glPushMatrix();
        {
            Vector3f v = ((ConvexInternalShape) body.shape()).implicitShapeDimensions; //HACK
            //adjust aspect ratio
            float r = 2f;
            float sx = v.x * r;
            float sy = v.y * r;
            float tx, ty;
            //if (sx > sy) {
                ty = sy;
                tx = sy/sx;
            //} else {
              //  tx = sx;
              //  ty = sx/sy;
            //}

            gl.glTranslatef(-1/4f, -1/4f, 0f); //align TODO not quite right yet

            gl.glScalef(tx, ty, 1f);


            renderRelativeAspect(gl);
        }
        gl.glPopMatrix();

        gl.glPopMatrix();
    }

    protected void renderRelative(GL2 gl, RigidBody body) {

        renderShape(gl, body);

    }
    protected void renderRelativeAspect(GL2 gl) {



    }

    protected void renderLabel(GL2 gl, float scale) {
        final float charAspect = 1.5f;
        ShapeDrawer.renderLabel(gl, scale, scale / charAspect, label, 0, 0, 0.5f);
    }

    protected void renderShape(GL2 gl, RigidBody body) {
        colorshape(gl);
        ShapeDrawer.draw(gl, body);
    }

    static float h(float p) {
        return p * 0.9f + 0.1f;
    }

    protected void colorshape(GL2 gl) {
        float p = h(pri)/2f;
        gl.glColor4f(p,
                //pri * Math.min(1f),
                p, //1f / (1f + (v.lag / (activationPeriods * dt)))),
                p,
                1f);
    }

    protected void renderAbsolute(GL2 gl) {
        renderEdges(gl, this);
    }

    static void renderEdges(GL2 gl, Spatial v) {
        int n = v.edgeCount();
        EDraw[] eee = v.edges;
        for (int en = 0; en < n; en++)
            render(gl, v, eee[en]);
    }

    static public void render(GL2 gl, Spatial v, EDraw e) {

        gl.glColor4f(e.r, e.g, e.b, e.a);

        float width = e.width * v.radius;
        if (width <= 1.1f) {
            renderLineEdge(gl, v, e, width);
        } else {
            renderHalfTriEdge(gl, v, e, width);
        }
    }

    static final Quaternion tmpQ = new Quaternion();
    static final Matrix4f tmpM4 = new Matrix4f();
    static final Matrix3f tmpM3 = new Matrix3f();

    static final float[] tmpV = new float[3];
    static final Vector3f ww = new Vector3f();
    static final Vector3f vv = new Vector3f();
    static final AxisAngle4f tmpA = new AxisAngle4f();

    static public void renderHalfTriEdge(GL2 gl, Spatial src, EDraw e, float width) {
        Spatial tgt = e.target;

        gl.glPushMatrix();

        src.transform().toAngleAxis(tmpQ, tmpA, ww);
        ww.normalize(); //used for the normal3f below
        float sx = src.x();
        float tx = tgt.x();
        float dx = tx - sx;
        float sy = src.y();
        float ty = tgt.y();
        float dy = ty - sy;
        float sz = src.z();
        float tz = tgt.z();
        float dz = tz - sz;

        vv.set(dx,dy,dz);
        vv.cross(ww, vv);
        vv.normalize();
        vv.scale(width);


        gl.glBegin(GL2.GL_TRIANGLES);


        gl.glVertex3f(sx+vv.x, sy+vv.y, sz+vv.z); //right base
        gl.glVertex3f( //right base
                //sx+-vv.x, sy+-vv.y, sz+-vv.z
                sx, sy, sz
        );
        gl.glVertex3f(tx, ty, tz); //right base
        gl.glEnd();

        gl.glPopMatrix();

    }

    public static void renderLineEdge(GL2 gl, Spatial src, EDraw e, float width) {
        Spatial tgt = e.target;
        gl.glLineWidth(width);
        gl.glBegin(GL.GL_LINES);
        Vector3f s = src.center;
        gl.glVertex3f(s.x, s.y, s.z);
        Vector3f t = tgt.center;
        gl.glVertex3f(t.x, t.y, t.z);
        gl.glEnd();
    }

    public <O> void stop(SpaceGraph s) {
        order = -1;
        preactive = false;
        body = null;
    }
}
