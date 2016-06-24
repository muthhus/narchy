package nars.gui.graph;

import bulletphys.collision.shapes.BoxShape;
import bulletphys.collision.shapes.CollisionShape;
import bulletphys.dynamics.RigidBody;
import bulletphys.ui.ShapeDrawer;
import bulletphys.ui.JoglPhysics;
import bulletphys.util.Motion;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.gl2.GLUT;
import nars.budget.Budget;
import nars.link.BLink;
import nars.task.Task;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.vecmath.Vector3f;
import java.util.function.BiConsumer;

import static com.jogamp.opengl.util.gl2.GLUT.STROKE_MONO_ROMAN;
import static nars.gui.graph.GraphSpace.h;
import static nars.gui.test.Lesson14.renderString;

/**
 * an atom (base unit) of spacegraph physics-simulated virtual matter
 */
public final class Atomatter implements BiConsumer<GL2, RigidBody> {


    public final nars.term.Termed key;
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

    public Budget budget;
    public float pri;

    /**
     * measure of inactivity, in time units
     */
    public float lag;

    /**
     * the draw order if being drawn
     */
    public short order;

    transient private GraphSpace grapher;

    transient public float radius;

    public final Motion motion = new Motion();
    public boolean motionLock;

    public Atomatter(nars.term.Termed k, int edges) {
        this.key = k;
        this.label = k.toString();
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
        shape = new BoxShape(JoglPhysics.v(1,1,1));
        center = motion.t.origin;

        inactivate();
    }



    @Override
    public boolean equals(Object obj) {
        return this == obj || key.equals(((Atomatter) obj).key);
    }

    @Override
    public int hashCode() {
        return hash;
    }


    transient int numEdges = 0;


    public boolean addTermLink(BLink<Termed> ll) {
        return addEdge(ll, ll.get(), false);
    }

    public boolean addTaskLink(BLink<Task> ll) {
        if (ll == null)
            return true;
        @Nullable Task t = ll.get();
        if (t == null)
            return true;
        return addEdge(ll, t.term(), true);
    }

    public boolean addEdge(BLink l, Termed ll, boolean task) {

        GraphSpace.EDraw[] ee = this.edges;

        Atomatter target = grapher.getIfActive(ll);
        if (target == null)
            return true;

        float pri = l.pri();
        float dur = l.dur();
        float qua = l.qua();

        float minLineWidth = 1f;
        float maxLineWidth = 7f;
        float width = minLineWidth + (maxLineWidth - minLineWidth) * ((dur) * (qua));

        float r, g, b;
        float hp = 0.4f + 0.6f * pri;
        //float qh = 0.5f + 0.5f * qua;
        if (task) {
            r = hp;
            g = dur/3f;
            b = 0;
        } else {
            b = hp;
            g = dur/3f;
            r = 0;
        }
        float a = 0.25f + 0.75f * (pri);

        int n;
        ee[n = (numEdges++)].set(target, width,
                r, g, b, a
        );
        return (n - 1 <= ee.length);
    }

    public void clearEdges(GraphSpace grapher) {
        this.numEdges = 0;
        this.grapher = grapher;
    }

    public boolean active() {
        return order >= 0;
    }

    public void activate(short order, BLink budget) {
        this.order = order;
        this.budget = budget;
    }

    public void inactivate() {
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

//        public void move(float tx, float ty, float tz, float rate) {
//
//            move(
//              Util.lerp(tx, x(), rate),
//              Util.lerp(ty, y(), rate),
//              Util.lerp(tz, z(), rate));
//        }

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
        this.shape.setLocalScaling(JoglPhysics.v(sx, sy, sz));
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

                body.setLinearVelocity(JoglPhysics.v());
                body.setDamping(0.99f, 0.5f);
                body.setFriction(0.9f);

                body.setUserPointer(this);

                body.setRenderer(this);
            }

        }

    }


    @Override public void accept(GL2 gl, RigidBody body) {

        gl.glPushMatrix();
        ShapeDrawer.translate(gl, body.transform());

        renderEdges(gl, this);

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

        float width = e.width;
        if (width <= 1.25f) {
            renderLineEdge(gl, v, e, width);
        } else {
            renderHalfTriEdge(gl, v, e, width);
        }
    }

    static public void renderHalfTriEdge(GL2 gl, Atomatter src, GraphSpace.EDraw e, float width) {
        Atomatter tgt = e.key;


        gl.glPushMatrix();

        {

            //TODO 3d line w/ correct normal calculation (ex: cross product)

            float x1 = src.x();
            float x2 = tgt.x();
            float dx = (x2 - x1);
            //float cx = 0.5f * (x1 + x2);
            float y1 = src.y();
            float y2 = tgt.y();
            float dy = (y2 - y1);
            //float cy = 0.5f * (y1 + y2);

            //gl.glTranslatef(cx, cy, 0f);

            float rotAngle = (float) Math.atan2(dy, dx) * 180f / 3.14159f;
            gl.glRotatef(rotAngle, 0f, 0f, 1f);


            float len = (float) Math.sqrt(dx * dx + dy * dy);
            gl.glScalef(len, width, 1f);

            //gl.glCallList(isoTri);
            gl.glBegin(GL2.GL_TRIANGLES);
            //gl.glNormal3f(0.0f, 0f, 1.0f);

            gl.glVertex3f(0, +0.5f,  0f); //right base
            gl.glVertex3f(0, -0.5f, 0f); //left base
            gl.glVertex3f(1,  0, 0f);  //midpoint on opposite end

            gl.glEnd();
        }

        gl.glPopMatrix();

    }

    public static void renderLineEdge(GL2 gl, Atomatter src, GraphSpace.EDraw e, float width) {
        Atomatter tgt = e.key;
        gl.glLineWidth(width);
        gl.glBegin(GL.GL_LINES);
        {
            gl.glVertex3f(0,0,0);//vp[0], vp[1], vp[2]);
            gl.glVertex3f(
                    tgt.x()-src.x(),
                    tgt.y()-src.y(),
                    tgt.z()-src.z() );
        }
        gl.glEnd();
    }

}
