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
import nars.budget.Budget;
import nars.link.BLink;
import nars.task.Task;
import nars.term.Termed;
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
    public final CollisionShape shape;

    transient private final Vector3f center; //references a field in MotionState's transform


    public String label;

    public O instance;
    public float pri;

    /**
     * measure of inactivity, in time units
     */
    public float lag;

    /**
     * the draw order if being drawn
     */
    transient public short order;


    transient public float radius;

    public final Motion motion = new Motion();
    public boolean motionLock;

    public Atomatter(O k, int edges) {
        this.key = k;
        this.label = toString();
        this.hash = k.hashCode();
        this.edges = new GraphSpace.EDraw[edges];
        this.radius = 0;

        final float initDistanceEpsilon = 0.5f;
        move(GraphSpace.r(initDistanceEpsilon),
             GraphSpace.r(initDistanceEpsilon),
             GraphSpace.r(initDistanceEpsilon));

        for (int i = 0; i < edges; i++)
            this.edges[i] = new GraphSpace.EDraw();

        //init physics
        shape = new BoxShape(Vector3f.v(1,1,1));
        center = motion.t.origin;

        inactivate();
    }

    @Override
    public String toString() {
        return key.toString();
    }

    public final Transform transform() { return motion.t; }


    @Override
    public boolean equals(Object obj) {
        return this == obj || key.equals(((Atomatter) obj).key);
    }

    @Override
    public int hashCode() {
        return hash;
    }


    public transient int numEdges = 0;



    public void clearEdges() {
        this.numEdges = 0;
    }

    public boolean active() {
        return order >= 0;
    }

    public final void activate(short order, O instance) {
        this.order = order;
        this.instance = instance;
    }

    public final void inactivate() {
        order = -1;
    }

    public void move(float x, float y, float z) {
        if (!motionLock) {

            if (body!=null) {
                body.transform().origin.set(x,y,z);

//                    com.Transform t = new com.Transform();
//                    body.getCenterOfMassTransform(t);
//                    t.origin.set(x, y, z);
//                    body.setCenterOfMassTransform(t);

                if (!body.isActive())
                    body.activate(true);
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
        this.shape.setLocalScaling(Vector3f.v(sx, sy, sz));
        this.radius = Math.max(Math.max(sx, sy), sz);
    }

    public void motionLock(boolean b) {
        motionLock = b;
    }

    public void update(GraphSpace graphSpace) {

        if (active()) {

            if (body == null) {
                final boolean collidesWithOthersLikeThis = false;
                body = graphSpace.newBody(
                        1f, //mass
                        shape, motion,
                        +1, //group
                        collidesWithOthersLikeThis ? -1 : -1 & ~(+1) //exclude collisions with self
                );

                body.setLinearVelocity(Vector3f.v());
                body.setDamping(0.99f, 0.5f);
                body.setFriction(0.9f);

                body.setUserPointer(this);

                body.setRenderer(this);
            }

        }

    }


    @Override public void accept(GL2 gl, RigidBody body) {

//        gl.glPushMatrix();

//        ShapeDrawer.translate(gl, body.transform());

        renderEdges(gl, this);

//        gl.glPopMatrix();

        gl.glPushMatrix();

        ShapeDrawer.transform(gl, body.transform());

        float p = h(pri)/2f;
        gl.glColor4f(p,
                //pri * Math.min(1f),
                p, //1f / (1f + (v.lag / (activationPeriods * dt)))),
                p,
                1f);

        ShapeDrawer.draw(gl, body);

        renderLabel(gl, this);


        gl.glPopMatrix();

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
