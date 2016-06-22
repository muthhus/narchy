package nars.gui.graph;

import com.google.common.collect.Lists;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.gl2.GLUT;
import nars.NAR;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.concept.Concept;
import nars.gui.graph.layout.FastOrganicLayout;
import nars.link.BLink;
import nars.nar.Default;
import nars.task.Task;
import nars.term.Termed;
import nars.util.JoglSpace;
import nars.util.Util;
import nars.util.data.list.FasterList;
import nars.util.experiment.DeductiveMeshTest;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.infinispan.commons.util.WeakValueHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static nars.gui.test.Lesson14.renderString;

/**
 * Created by me on 6/20/16.
 */
public class GraphSpace extends JoglSpace {


    public static void main(String[] args) {

        Default n = new Default(1024, 8, 4, 3);

        //n.log();

        new DeductiveMeshTest(n, new int[]{5, 5}, 16384);


        final int maxNodes = 256;

        new GraphSpace(new ConceptsSource(n, maxNodes)).show(900, 900);
        n.loop(25f);

    }


    //private final GleemControl gleem = new GleemControl();

    final FasterList<ConceptsSource> sources = new FasterList<>(1);
    final WeakValueHashMap<Termed, VDraw> vdraw;

    int maxEdgesPerVertex = 6;

    List<GraphLayout> layout = Lists.newArrayList(
        //new Spiral()
        new FastOrganicLayout()
    );

    private int box, isoTri;

    public GraphSpace(ConceptsSource c) {
        super();

        vdraw = new WeakValueHashMap<>(1024);

        enable(c);
    }

    public void enable(ConceptsSource c) {
        sources.add(c);
        c.start(this);
    }

    @NotNull
    public VDraw update(int order, BLink<? extends Termed> t) {
        return pre(order, getOrAdd(t.get()), t);
    }

    @NotNull
    public VDraw getOrAdd(Termed t) {
        return vdraw.computeIfAbsent(t, x -> new VDraw(x, maxEdgesPerVertex));
    }

    @Nullable
    public VDraw getIfActive(Termed t) {
        VDraw v = vdraw.get(t);
        return v != null && v.active() ? v : null;
    }

    /**
     * get the latest info into the draw object
     */
    @NotNull
    protected VDraw pre(int i, VDraw v, BLink<? extends Termed> b) {
        v.order = (short)i;
        v.budget = b;
        return v;
    }

    protected void post(float now, VDraw v) {
        Termed tt = v.key;

        Budget b = v.budget;
        float p = v.pri = b.priIfFiniteElseZero();

        float nodeScale = 0.1f + 2f * p;
        nodeScale /= Math.sqrt(tt.volume());
        v.scale(nodeScale, nodeScale, nodeScale/2f);

        if (tt instanceof Concept) {
            updateConcept(v, (Concept) tt, now);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void updateConcept(VDraw v, Concept cc, float now) {

        float lastConceptForget = v.budget.getLastForgetTime();
        if (lastConceptForget != lastConceptForget)
            lastConceptForget = now;

        @NotNull Bag<Termed> termlinks = cc.termlinks();
        @NotNull Bag<Task> tasklinks = cc.tasklinks();
//
//        if (!termlinks.isEmpty()) {
//            float lastTermlinkForget = ((BLink) (((ArrayBag) termlinks).get(0))).getLastForgetTime();
//            if (lastTermlinkForget != lastTermlinkForget)
//                lastTermlinkForget = lastConceptForget;
//        }

        //v.lag = now - Math.max(lastConceptForget, lastTermlinkForget);
        v.lag = now - lastConceptForget;
        //float act = 1f / (1f + (timeSinceLastUpdate/3f));

        v.clearEdges(this);
        int maxEdges = v.edges.length;

        tasklinks.topWhile(v::addTaskLink, maxEdges / 2);
        termlinks.topWhile(v::addTermLink, maxEdges - v.edgeCount()); //fill remaining edges

    }


    public static final class EDraw {
        public VDraw key;
        public float width, r, g, b, a;

        public void set(VDraw x, float width, float r, float g, float b, float a) {
            this.key = x;
            this.width = width;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        public void clear() {
            key = null;
        }
    }


    /**
     * vertex draw info
     */
    public static final class VDraw {
        public final nars.term.Termed key;
        public final int hash;
        @NotNull public final EDraw[] edges;

        /** position: x, y, z */
        @NotNull public final float p[] = new float[3];

        /** scale: x, y, z -- should not modify directly, use scale(x,y,z) method to change */
        @NotNull public final float[] s = new float[3];


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

        transient private float radius;


        public VDraw(nars.term.Termed k, int edges) {
            inactivate();
            this.key = k;
            this.label = k.toString();
            this.hash = k.hashCode();
            this.edges = new EDraw[edges];
            this.radius = 0;

            final float initDistanceEpsilon = 0.5f;
            move(r(initDistanceEpsilon),
                 r(initDistanceEpsilon),
                 r(initDistanceEpsilon));

            for (int i = 0; i < edges; i++)
                this.edges[i] = new EDraw();

        }

        static float r(float range) {
            return (-0.5f + (float)Math.random()*range)*2f;
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

            EDraw[] ee = this.edges;

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

        public void inactivate() {
            order = -1;
        }

        public void move(float x, float y, float z) {
            float[] p = this.p;
            p[0] = x;
            p[1] = y;
            p[2] = z;
        }

        public void move(float tx, float ty, float tz, float rate) {
            float[] p = this.p;
            p[0] = Util.lerp(tx, p[0], rate);
            p[1] = Util.lerp(ty, p[1], rate);
            p[2] = Util.lerp(tz, p[2], rate);
        }

        public int edgeCount() {
            return numEdges;
        }

        public float x() {  return p[0];        }
        public float y() {  return p[1];        }
        public float z() {  return p[2];        }



        public void moveDelta(float dx, float dy, float dz) {
            float[] p = this.p;
            p[0] += dx;
            p[1] += dy;
            p[2] += dz;
        }

        public void scale(float sx, float sy, float sz) {
            float[] s = this.s;
            s[0] = sx;
            s[1] = sy;
            s[2] = sz;
            this.radius = Math.max(Math.max(sx, sy), sz);
        }

        public float radius() {
            return radius;
        }

    }

    public void init(GL2 gl) {

        //gl.glEnable(GL2.GL_TEXTURE_2D); // Enable Texture Mapping

        gl.glShadeModel(GL2.GL_SMOOTH); // Enable Smooth Shading
        gl.glShadeModel(GL2.GL_LINE_SMOOTH); // Enable Smooth Shading

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.01f); // Black Background
        gl.glClearDepth(1f); // Depth Buffer Setup
        gl.glEnable(GL2.GL_DEPTH_TEST); // Enables Depth Testing
        gl.glDepthFunc(GL2.GL_LEQUAL);

        // Quick And Dirty Lighting (Assumes Light0 Is Set Up)
        //gl.glEnable(GL2.GL_LIGHT0);

        //gl.glEnable(GL2.GL_LIGHTING); // Enable Lighting

        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);


        gl.glEnable(GL2.GL_COLOR_MATERIAL);


        //gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST); // Really Nice Perspective Calculations

        //loadGLTexture(gl);
        buildLists(gl);




//        gleem.start(Vec3f.Y_AXIS, window);
//        gleem.attach(new DefaultHandleBoxManip(gleem).translate(0, 0, 0));
    }

    private void buildLists(GL2 gl) {
        box = gl.glGenLists(2); // Generate 2 Different Lists
        gl.glNewList(box, GL2.GL_COMPILE); // Start With The Box List

        gl.glBegin(GL2.GL_QUADS);
        gl.glNormal3f(0.0f, -1.0f, 0.0f);
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(-1.0f, -1.0f, -1.0f); // Bottom Face
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(1.0f, -1.0f, -1.0f);
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(1.0f, -1.0f, 1.0f);
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(-1.0f, -1.0f, 1.0f);

        gl.glNormal3f(0.0f, 0.0f, 1.0f);
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(-1.0f, -1.0f, 1.0f); // Front Face
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(1.0f, -1.0f, 1.0f);
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(1.0f, 1.0f, 1.0f);
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(-1.0f, 1.0f, 1.0f);

        gl.glNormal3f(0.0f, 0.0f, -1.0f);
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(-1.0f, -1.0f, -1.0f); // Back Face
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(-1.0f, 1.0f, -1.0f);
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(1.0f, 1.0f, -1.0f);
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(1.0f, -1.0f, -1.0f);

        gl.glNormal3f(1.0f, 0.0f, 0.0f);
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(1.0f, -1.0f, -1.0f); // Right face
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(1.0f, 1.0f, -1.0f);
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(1.0f, 1.0f, 1.0f);
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(1.0f, -1.0f, 1.0f);

        gl.glNormal3f(-1.0f, 0.0f, 0.0f);
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(-1.0f, -1.0f, -1.0f); // Left Face
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(-1.0f, -1.0f, 1.0f);
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(-1.0f, 1.0f, 1.0f);
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(-1.0f, 1.0f, -1.0f);
        gl.glEnd();

        gl.glEndList();

        {
            isoTri = box + 1; // Storage For "Top" Is "Box" Plus One
            gl.glNewList(isoTri, GL2.GL_COMPILE); // Now The "Top" Display List

            gl.glBegin(GL2.GL_TRIANGLES);
            gl.glNormal3f(0.0f, 0f, 1.0f);

            final float h = 0.5f;
            gl.glVertex3f(0, -h, 0f); //left base
            gl.glVertex3f(0, h,  0f); //right base
            gl.glVertex3f(1,  0, 0f);  //midpoint on opposite end

            gl.glEnd();
            gl.glEndList();
        }
    }


    float r0 = 0f;

    public synchronized void display(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();

        clear(gl);


        updateCamera(gl);



        List<ConceptsSource> s = this.sources;
        for (int i = 0, sourcesSize = s.size(); i < sourcesSize; i++) {
            render(gl, s.get(i));
        }

//        gleem.viewAll();
//        gleem.render(gl);
    }

    public void clear(GL2 gl) {
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
    }

    public void render(GL2 gl, ConceptsSource s) {

        @Deprecated float dt = Math.max(0.001f /* non-zero */, s.dt());

        List<VDraw> toDraw = s.visible;

        s.busy.set(true);

        update(toDraw, dt);

        for (int i1 = 0, toDrawSize = toDraw.size(); i1 < toDrawSize; i1++) {

            render(gl, dt, toDraw.get(i1));

        }

        s.busy.set(false);
    }

    public void render(GL2 gl, float dt, VDraw v) {

        gl.glPushMatrix();


        float[] pp = v.p;
        float x = pp[0], y = pp[1], z = pp[2];
        gl.glTranslatef(x, y, z);

        renderVertexBase(gl, dt, v);

        renderEdges(gl, v);

        renderLabel(gl, v);

        gl.glPopMatrix();


    }

    public void renderEdges(GL2 gl, VDraw v) {
        int n = v.edgeCount();
        EDraw[] eee = v.edges;
        for (int en = 0; en < n; en++)
            render(gl, v, eee[en]);
    }

    public void renderVertexBase(GL2 gl, float dt, VDraw v) {

        gl.glPushMatrix();


        //gl.glRotatef(45.0f - (2.0f * yloop) + xrot, 1.0f, 0.0f, 0.0f);
        //gl.glRotatef(45.0f + yrot, 0.0f, 1.0f, 0.0f);


        float pri = v.pri;


        float[] s = v.s;
        gl.glScalef(s[0], s[1], s[2]);

        final float activationPeriods = 4f;
        gl.glColor4f(h(pri),
                pri * Math.min(1f, 1f / (1f + (v.lag / (activationPeriods * dt)))),
                h(v.budget.dur()),
                v.budget.qua() * 0.25f + 0.25f);
        gl.glCallList(box);
        //glut.glutSolidTetrahedron();

        gl.glPopMatrix();
    }

    public void renderLabel(GL2 gl, VDraw v) {


        //float p = v.pri * 0.75f + 0.25f;
        gl.glColor4f(0.5f, 0.5f, 0.5f, 1f);

        float fontThick = 2f;
        gl.glLineWidth(fontThick);

        float div = 0.01f;
        renderString(gl, GLUT.STROKE_ROMAN /*STROKE_MONO_ROMAN*/, v.label,
                div * v.s[0], //scale
                0, 0, (3.5f * v.s[2])/div); // Print GL Text To The Screen


    }

    public void render(GL2 gl, VDraw v, EDraw e) {

        gl.glColor4f(e.r, e.g, e.b, e.a);
        float width = e.width;
        if (width <= 1f) {
            renderLineEdge(gl, v, e, width);
        } else {
            renderHalfTriEdge(gl, v, e, width);
        }
    }

    public void renderHalfTriEdge(GL2 gl, VDraw v, EDraw e, float width) {
        VDraw ee = e.key;
        float[] tgt = ee.p;
        float[] src = v.p;


        gl.glPushMatrix();

        {

            float x1 = src[0];
            float x2 = tgt[0];
            float dx = (x2 - x1);
            //float cx = 0.5f * (x1 + x2);
            float y1 = src[1];
            float y2 = tgt[1];
            float dy = (y2 - y1);
            //float cy = 0.5f * (y1 + y2);

            //gl.glTranslatef(cx, cy, 0f);

            float rotAngle = (float) Math.atan2(dy, dx) * 180f / 3.14159f;
            gl.glRotatef(rotAngle, 0f, 0f, 1f);


            float len = (float) Math.sqrt(dx * dx + dy * dy);
            gl.glScalef(len, width, 1f);

            gl.glCallList(isoTri);
        }

        gl.glPopMatrix();

    }

    public static void renderLineEdge(GL2 gl, VDraw v, EDraw e, float width) {
        VDraw ee = e.key;
        float[] eep = ee.p;
        float[] vp = v.p;
        gl.glLineWidth(width);
        gl.glBegin(GL.GL_LINES);
        {
            gl.glVertex3f(0,0,0);//vp[0], vp[1], vp[2]);
            gl.glVertex3f(eep[0]-vp[0], eep[1]-vp[1], eep[2]-vp[2]);
        }
        gl.glEnd();
    }

    public void update(List<VDraw> toDraw, float dt) {
        List<GraphLayout> ll = this.layout;
        for (int i1 = 0, layoutSize = ll.size(); i1 < layoutSize; i1++) {
            ll.get(i1).update(this, toDraw, dt);
        }
    }

    public void updateCamera(GL2 gl) {
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
//        gl.glRotatef(0,1,0,0);
//        gl.glRotatef(0,0,1,0);
//        gl.glRotatef(0,0,0,1);
//        gl.glScalef(1f,1f,1f);
        gl.glTranslatef(0, 0, -90f);
//        gl.glRotatef(r0,    1.0f, 0.0f, 0.0f);
//        gl.glRotatef(-r0/1.5f, 0.0f, 1.0f, 0.0f);
//        gl.glRotatef(r0 / 2f, 0.0f, 0.0f, 1.0f);
//        r0 += 0.3f;
    }

    public float h(float p) {
        return p * 0.9f + 0.1f;
    }

    public void reshape(GLAutoDrawable drawable,
                        int xstart,
                        int ystart,
                        int width,
                        int height) {

        height = (height == 0) ? 1 : height;

        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL2ES1.GL_PROJECTION);
        gl.glLoadIdentity();

        glu.gluPerspective(45, (float) width / height, 10, 5000);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

//        gleem.viewAll();

    }

    public static class ConceptsSource {


        public final NAR nar;
        private final int capacity;


        private FasterList<VDraw> visible = new FasterList(0);


        public final MutableFloat maxPri = new MutableFloat(1.0f);
        public final MutableFloat minPri = new MutableFloat(0.0f);


        //private String keywordFilter;
        //private final ConceptFilter eachConcept = new ConceptFilter();

        final AtomicBoolean busy = new AtomicBoolean(true);
        private long now;
        private GraphSpace grapher;
        private float dt;

        public ConceptsSource(NAR nar, int maxNodes) {


            this.nar = nar;
            now = nar.time();

            this.capacity = maxNodes;

            nar.onFrame(nn -> {
                if (!busy.get()) {
                    update();
                }
            });

//            includeString.addListener((e) -> {
//                //System.out.println(includeString.getValue());
//                //setUpdateable();
//            });
        }

        public void start(GraphSpace grapher) {
            this.grapher = grapher;
        }

        public void stop() {
            this.grapher = null;
        }

//        @Override
//        public void updateEdge(TermEdge ee, Object link) {
//            //rolling average
//        /*ee.pri = lerp(
//                ((BLink)link).pri(), ee.pri,
//                      0.1f);*/
//
//            ee.pri.addValue( ((BLink) link).pri() );
//        }


//        @Override
//        public void updateNode(SpaceGrapher g, Termed s, TermNode sn) {
//            sn.pri(nar.conceptPriority(s));
//            super.updateNode(g, s, sn);
//        }
//
//        @Override
//        public void forEachOutgoingEdgeOf(Termed cc,
//                                          Consumer eachTarget) {
//
//
//            SpaceGrapher sg = grapher;
////        if (sg == null)
////            throw new RuntimeException("grapher null");
//
//
//            Term cct = cc.term();
//
//
//            final int[] count = {0};
//            final int[] max = {0};
//            Predicate linkUpdater = link -> {
//
//                Termed target = ((BLink<Termed>) link).get();
//
//                //if (cct.equals(target)) //self-loop
//                //    return true;
//
//                TermNode tn = sg.getTermNode(target);
//                if (tn != null) {
//                    eachTarget.accept(link); //tn.c);
//                    return (count[0]++) < max[0];
//                }
//
//                return true;
//
////            TermEdge.TLinkEdge ee = (TermEdge.TLinkEdge) getEdge(sg, sn, tn, edgeBuilder);
////
////            if (ee != null) {
////                ee.linkFrom(tn, link);
////            }
////
////            //missing.remove(tn.term);
//            };
//
//            max[0] = maxNodeLinks;
//            ((Concept) cc).termlinks().topWhile(linkUpdater);
//            max[0] = maxNodeLinks; //equal chance for both link types
//            ((Concept) cc).tasklinks().topWhile(linkUpdater);
//
//            //sn.removeEdges(missing);
//
//        }
//
//        @Override
//        public Termed getTargetVertex(Termed edge) {
//
//            return grapher.getTermNode(edge).c;
//        }
//
//
//        @Override
//        public void start(SpaceGrapher g) {
//
//            if (g != null) {
//
//                //.stdout()
//                //.stdoutTrace()
//                //                .input("<a --> b>. %1.00;0.7%", //$0.9;0.75;0.2$
//                //                        "<b --> c>. %1.00;0.7%")
//
//                if (regs != null)
//                    throw new RuntimeException("already started");
//
//
//                regs = new Active(
//                    /*nar.memory.eventConceptActivated.on(
//                            c -> refresh.set(true)
//                    ),*/
//                        nar.eventFrameStart.on(h -> {
//                            //refresh.set(true);
//                            updateGraph();
//                        })
//                );
//
//                super.start(g);
//            } else {
//                if (regs == null)
//                    throw new RuntimeException("already stopped");
//
//                regs.off();
//                regs = null;
//            }
//        }


        protected void update() {
            float last = this.now;
            this.dt = (this.now = nar.time()) - last;

            //String _keywordFilter = includeString.get();
            //this.keywordFilter = _keywordFilter != null && _keywordFilter.isEmpty() ? null : _keywordFilter;

            //_minPri = this.minPri.floatValue();
            //_maxPri = this.maxPri.floatValue();

            //final int maxNodes = this.maxNodes;


            visible.forEach(VDraw::inactivate);

            FasterList<VDraw> v = visible = new FasterList(capacity);
            Bag<Concept> x = ((Default) nar).core.concepts;
            x.topWhile(this::accept, capacity);

            GraphSpace g = grapher;
            long now = this.now;
            for (int i1 = 0, toDrawSize = v.size(); i1 < toDrawSize; i1++) {
                g.post(now, v.get(i1));
            }
        }

        public boolean accept(BLink<Concept> b) {

            float pri = b.pri();
            if (pri != pri) {
                //throw new RuntimeException("deleted item: " + b);
                return true;
            }

            FasterList<VDraw> v = this.visible;
            return v.add(grapher.update(v.size(), b));
        }

        public long time() {
            return nar.time();
        }

        public float dt() {
            return dt;
        }

//        private class ConceptFilter implements Predicate<BLink<Concept>> {
//
//            int count;
//
//            public void reset() {
//                count = 0;
//            }
//
//            @Override
//            public boolean test(BLink<Concept> cc) {
//
//
//                float p = cc.pri();
//                if ((p < _minPri) || (p > _maxPri)) {
//                    return true;
//                }
//
//                Concept c = cc.get();
//
//                String keywordFilter1 = keywordFilter;
//                if (keywordFilter1 != null) {
//                    if (!c.toString().contains(keywordFilter1)) {
//                        return true;
//                    }
//                }
//
//                concepts.add(c);
//                return count++ <= maxNodes;
//
//            }
//        }


//    public static void updateConceptEdges(SpaceGrapher g, TermNode s, TLink link, DoubleSummaryReusableStatistics accumulator) {
//
//
//        Term t = link.getTerm();
//        TermNode target = g.getTermNode(t);
//        if ((target == null) || (s.equals(target))) return;
//
//        TermEdge ee = getConceptEdge(g, s, target);
//        if (ee != null) {
//            ee.linkFrom(s, link);
//            accumulator.accept(link.getPriority());
//        }
//    }


//    public final void updateNodeOLD(SpaceGrapher sg, BagBudget<Concept> cc, TermNode sn) {
//
//        sn.c = cc.get();
//        sn.priNorm = cc.getPriority();
//
//
//
//        //final Term t = tn.term;
//        //final DoubleSummaryReusableStatistics ta = tn.taskLinkStat;
//        //final DoubleSummaryReusableStatistics te = tn.termLinkStat;
//
//
////        System.out.println("refresh " + Thread.currentThread() + " " + termLinkMean.getResult() + " #" + termLinkMean.getN() );
//
//
////        Consumer<TLink> tLinkConsumer = t -> {
////            Term target = t.getTerm();
////            if (!source.equals(target.getTerm())) {
////                TermNode tn = getTermNode(graph, target);
////                //TermEdge edge = getConceptEdge(graph, sn, tn);
////
////            }
////        };
////
////        c.getTaskLinks().forEach(tLinkConsumer);
////        c.getTermLinks().forEach(tLinkConsumer);
//
//
//    }


    }

}
