package nars.gui.graph;

import bulletphys.collision.shapes.BoxShape;
import bulletphys.collision.shapes.CollisionShape;
import bulletphys.dynamics.RigidBody;
import bulletphys.linearmath.Transform;
import bulletphys.ui.ShapeDrawer;
import bulletphys.util.Motion;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.Quaternion;
import org.jetbrains.annotations.NotNull;

import javax.vecmath.Vector3f;
import java.util.function.BiConsumer;

import static com.jogamp.opengl.util.gl2.GLUT.STROKE_MONO_ROMAN;
import static nars.gui.graph.GraphSpace.h;
import static nars.gui.test.Lesson14.renderString;

/**
 * an atom (base unit) of spacegraph physics-simulated virtual matter
 */
public class Atomatter<O> implements BiConsumer<GL2, RigidBody> {


    public final O key;
    public final int hash;

    @NotNull
    public final GraphSpace.EDraw[] edges;

    /** position: x, y, z */
    //@NotNull public final float p[] = new float[3];

    /** scale: x, y, z -- should not modify directly, use scale(x,y,z) method to change */
    //@NotNull public final float[] s = new float[3];

    public RigidBody body;

    transient private final Vector3f center; //references a field in MotionState's transform


    public String label;

    public float pri;



    /**
     * the draw order if being drawn
     */
    transient public short order;


    transient public float radius;

    public final Motion motion = new Motion();
    public boolean motionLock;

    public Atomatter() {
        this(null);
    }

    public Atomatter(O k) {
        this(k, 0);
    }

    @Deprecated public Atomatter(O k, int edges) {
        this.key = k!=null ? k : (O) this;
        this.label = key!=null ? key.toString() : super.toString();
        this.hash = k!=null ? k.hashCode() : super.hashCode();
        this.edges = new GraphSpace.EDraw[edges];
        this.radius = 0;
        this.pri = 0.5f;

        final float initDistanceEpsilon = 0.5f;
        move(GraphSpace.r(initDistanceEpsilon),
             GraphSpace.r(initDistanceEpsilon),
             GraphSpace.r(initDistanceEpsilon));

        for (int i = 0; i < edges; i++)
            this.edges[i] = new GraphSpace.EDraw();

        //init physics
        center = motion.t.origin;

        inactivate();
    }

    @Override
    public String toString() {
        return label;
    }

    public final Transform transform() { return motion.t; }


    @Override
    public final boolean equals(Object obj) {
        return this == obj || key.equals(((Atomatter) obj).key);
    }

    @Override
    public final int hashCode() {
        return hash;
    }


    @Deprecated public transient int numEdges = 0;



    public void clearEdges() {
        this.numEdges = 0;
    }

    public boolean active() {
        return order >= 0;
    }

    public final void activate(short order) {
        this.order = order;
    }

    public final void inactivate() {
        order = -1;
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

                if (!b.isActive())
                    b.activate(true);
            } else {
                motion.t.origin.set(x, y, z);
            }
        }

//            float[] p = this.p;
//            p[0] = x;
//            p[1] = y;
//            p[2] = z;
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
        if (body!=null)
            this.body.shape().setLocalScaling(Vector3f.v(sx, sy, sz));
        this.radius = Math.max(Math.max(sx, sy), sz);
    }

    public void motionLock(boolean b) {
        motionLock = b;
    }

    public void update(GraphSpace graphSpace) {

        if (active()) {

            if (body == null) {
                RigidBody b = body = newBody(graphSpace, newShape(), false);
                b.setUserPointer(this);
                b.setRenderer(this);
            }

        }

    }

    //TODO make abstract
    protected CollisionShape newShape() {
        return new BoxShape(Vector3f.v(1, 1, 1));
    }

    public RigidBody newBody(GraphSpace graphSpace) {
        final boolean collidesWithOthersLikeThis = false;
        return newBody(graphSpace, newShape(), collidesWithOthersLikeThis);
    }

    public RigidBody newBody(GraphSpace graphSpace, CollisionShape shape, boolean collidesWithOthersLikeThis) {
        RigidBody b;
        b = graphSpace.newBody(
                1f, //mass
                shape, motion,
                +1, //group
                collidesWithOthersLikeThis ? -1 : -1 & ~(+1) //exclude collisions with self
        );

        b.setDamping(0.99f, 0.5f);
        b.setFriction(0.9f);
        return b;
    }


    @Override public final void accept(GL2 gl, RigidBody body) {

        renderAbsolute(gl);

        gl.glPushMatrix();
        ShapeDrawer.transform(gl, body.transform());
        renderRelative(gl, body);
        gl.glPopMatrix();
    }

    protected void renderRelative(GL2 gl, RigidBody body) {

        renderShape(gl, body);

    }

    protected void renderLabel(GL2 gl) {
        renderLabel(gl, this);
    }

    protected void renderShape(GL2 gl, RigidBody body) {
        float p = h(pri)/2f;
        gl.glColor4f(p,
                //pri * Math.min(1f),
                p, //1f / (1f + (v.lag / (activationPeriods * dt)))),
                p,
                1f);
        ShapeDrawer.draw(gl, body);
    }

    protected void renderAbsolute(GL2 gl) {
        renderEdges(gl, this);
    }

    static void renderEdges(GL2 gl, Atomatter v) {
        int n = v.edgeCount();
        GraphSpace.EDraw[] eee = v.edges;
        for (int en = 0; en < n; en++)
            render(gl, v, eee[en]);
    }

    static public void renderLabel(GL2 gl, Atomatter v) {


        //float p = v.pri * 0.75f + 0.25f;
        gl.glColor4f(0.5f, 0.5f, 0.5f, v.pri);

        float fontThick = 1f;
        gl.glLineWidth(fontThick);

        float div = 0.003f;
        //float r = v.radius;
        renderString(gl, /*GLUT.STROKE_ROMAN*/ STROKE_MONO_ROMAN, v.label,
                div, //scale
                0, 0, (1/1.9f)/div); // Print GL Text To The Screen

    }

    static public void render(GL2 gl, Atomatter v, GraphSpace.EDraw e) {

        gl.glColor4f(e.r, e.g, e.b, e.a);

        float width = e.width * v.radius;
        if (width <= 1.5f) {
            renderLineEdge(gl, v, e, width);
        } else {
            renderHalfTriEdge(gl, v, e, width);
        }
    }

    static final Quaternion tmpQ = new Quaternion();
    static final float[] tmpV = new float[3];
    static final Vector3f ww = new Vector3f();
    static final Vector3f vv = new Vector3f();

    static public void renderHalfTriEdge(GL2 gl, Atomatter src, GraphSpace.EDraw e, float width) {
        Atomatter tgt = e.key;

        gl.glPushMatrix();

        {
            float a = src.transform().getRotation(tmpQ).toAngleAxis(tmpV);
            ww.set(tmpV);
            //ww.normalize(); //used for the normal3f below
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


            //gl.glNormal3f(ww.x, ww.y, ww.z);

            gl.glVertex3f(sx+vv.x, sy+vv.y, sz+vv.z); //right base
            gl.glVertex3f( //right base
                    //sx+-vv.x, sy+-vv.y, sz+-vv.z
                    sx, sy, sz
            );
            gl.glVertex3f(tx, ty, tz); //right base
            gl.glEnd();

        }

        gl.glPopMatrix();

    }

    public static void renderLineEdge(GL2 gl, Atomatter src, GraphSpace.EDraw e, float width) {
        Atomatter tgt = e.key;
        gl.glLineWidth(width);
        gl.glBegin(GL.GL_LINES);
        {
            Vector3f s = src.center;
            gl.glVertex3f(s.x, s.y, s.z);
            Vector3f t = tgt.center;
            gl.glVertex3f(t.x, t.y, t.z);
        }
        gl.glEnd();
    }

}
