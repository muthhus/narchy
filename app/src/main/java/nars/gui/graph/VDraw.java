package nars.gui.graph;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.ui.JoglPhysics;
import com.bulletphysics.util.Motion;
import com.jogamp.opengl.GL2;
import nars.budget.Budget;
import nars.link.BLink;
import nars.task.Task;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.vecmath.Vector3f;

/**
 * vertex draw info
 */
public final class VDraw {
    private GraphSpace graphSpace;
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

    public VDraw(GraphSpace graphSpace, nars.term.Termed k, int edges) {
        this.graphSpace = graphSpace;
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
        return this == obj || key.equals(((VDraw) obj).key);
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

        VDraw target = grapher.getIfActive(ll);
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

    public void render(GL2 gl, float dt) {
        //renderVertexBase(gl, dt, v);

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
        }

        graphSpace.renderEdges(gl, this);

        graphSpace.renderLabel(gl, this);

    }

    public void preDraw(GL2 gl) {
        float p = graphSpace.h(pri)/2f;
        gl.glColor4f(p,
                //pri * Math.min(1f),
                p, //1f / (1f + (v.lag / (activationPeriods * dt)))),
                p,
                1f);
    }
}
