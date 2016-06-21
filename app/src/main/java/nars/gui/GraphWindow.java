package nars.gui;

import com.google.common.collect.Lists;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import nars.NAR;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.Default;
import nars.task.Task;
import nars.term.Termed;
import nars.util.AbstractJoglWindow;
import nars.util.data.Util;
import nars.util.data.list.FasterList;
import nars.util.experiment.DeductiveMeshTest;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.infinispan.commons.util.WeakValueHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static nars.gui.tutorial.Lesson14.renderString;

/**
 * Created by me on 6/20/16.
 */
public class GraphWindow extends AbstractJoglWindow {


    public static void main(String[] args) {

        Default n = new Default(1024, 8, 3, 3);

        //n.log();

        new DeductiveMeshTest(n, new int[]{6, 5}, 16384);

//        n.input("<a <-> b>. :|:");
//        n.step();
//        n.input("<b --> c>. :|:");
//        n.step();
//        n.input("<c --> d>. :|:");
//        n.step();
//        n.input("<d --> a>. :|:");
//        n.step();
//        n.input("<(a,b) <-> c>?");
//        n.step();
//        n.input("<(a,b,#x) <-> d>!");
//        n.step();
//        n.input("<(a,?x,d) <-> e>. :|:");
//        n.input("<(a,?x,?y) --> {e,d,a}>. :|:");
//        n.step();
//        n.input("wtf(1,2,#x)!");
//        //n.input("<(c|a) --> (b&e)>! :|:");
//        //n.step();
//        //n.input("<x <-> a>! :|:");
//        //n.run(5);


        final int maxNodes = 1024;

        new GraphWindow(new ConceptsSource(n, maxNodes)).show(500, 500);
        n.loop(25f);

    }

    private static final GLU glu = new GLU();
    private static final GLUT glut = new GLUT();

    final FasterList<ConceptsSource> sources = new FasterList<>(1);
    final WeakValueHashMap<Termed, VDraw> vdraw;

    int maxEdgesPerVertex = 8;

    List<GraphLayout> layout = Lists.newArrayList(
        new Spiral()
    );

    private int box, top;

    public GraphWindow(ConceptsSource c) {
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
        v.order = i;
        v.budget = b; //mark as active
        return v;
    }

    protected void post(float now, VDraw v) {
        Termed tt = v.key;

        Budget b = v.budget;
        v.pri = b.priIfFiniteElseZero();

        if (tt instanceof Concept) {
            updateEdges(v, (Concept) tt, now);
        }
    }

    private void updateEdges(VDraw v, Concept cc, float now) {

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
        termlinks.topWhile(v::addTermLink, maxEdges - v.numEdges()); //fill remaining edges


    }

    public interface GraphLayout {

        void update(GraphWindow g, List<VDraw> verts, float dt);

    }

    public static class Spiral implements GraphLayout {

        float nodeSpeed = 0.05f;

        @Override
        public void update(GraphWindow g, List<VDraw> verts, float dt) {
            verts.forEach(this::update);
        }

        protected void update(VDraw v) {
            //TODO abstract
            //int hash = v.hash;
            //int vol = v.key.volume();

            //float ni = n / (float) Math.E;
            //final float bn = 1f;

            float baseRad = 5f;
            //float p = v.pri;

            float nodeSpeed = (this.nodeSpeed / (1f + v.pri));

            int o = v.order;
            float theta = o;

            v.move(
                    (float) Math.sin(theta / 10f) * (baseRad + 0.2f * (theta)),
                    (float) Math.cos(theta / 10f) * (baseRad + 0.2f * (theta)),
                    0,
                    //1f/(1f+v.lag) * (baseRad/2f);
                    //v.budget.qua() * (baseRad + rad)
                    //v.tp[2] = act*10f;
                    nodeSpeed);

        }

    }



    public static class EDraw {
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
    public static class VDraw {
        public final nars.term.Termed key;
        private final int hash;
        private final EDraw[] edges;

        /**
         * current x, y, z
         */
        public float p[] = new float[3];

        /**
         * target x, y, z
         */
        public float tp[] = new float[3];

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
        public int order;

        transient private GraphWindow grapher;

        public VDraw(nars.term.Termed k, int edges) {
            inactivate();
            this.key = k;
            this.label = k.toString();
            this.hash = k.hashCode();
            this.edges = new EDraw[edges];
            for (int i = 0; i < edges; i++)
                this.edges[i] = new EDraw();
        }

        @Override
        public boolean equals(Object obj) {
            return key.equals(((VDraw) obj).key);
        }

        @Override
        public int hashCode() {
            return hash;
        }


        /**
         * nodeSpeed < 1.0
         */
        public void updatePosition(float nodeSpeed) {
            float[] p = this.p;
            float[] tp = this.tp;
            for (int i = 0; i < 3; i++) {
                p[i] = Util.lerp(tp[i], p[i], nodeSpeed);
            }
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
            if (numEdges >= ee.length)
                return false;

            VDraw target = grapher.getIfActive(ll);
            if (target == null)
                return true;

            float pri = l.pri();
            float dur = l.dur();
            float qua = l.qua();
            float baseLineWidth = 3f;

            float width = baseLineWidth * (1f + pri) * (1f + dur);
            float r, g, b;
            float hp = 0.5f + 0.5f * pri;
            if (task) {
                r = hp;
                g = qua * hp;
                b = 0;
            } else {
                b = hp;
                g = qua * hp;
                r = 0;
            }

            ee[numEdges].set(target, width, r, g, b, 0.75f + 0.25f * (dur));
            numEdges++;

            return true;
        }

        public void clearEdges(GraphWindow grapher) {
            numEdges = 0;
            this.grapher = grapher;
        }

        public boolean active() {
            return order >= 0;
        }

        public void inactivate() {
            order = -1;
        }

        public void move(float x, float y, float z, float nodeSpeed) {
            float[] t = this.tp;
            t[0] = x;
            t[1] = y;
            t[2] = z;
            updatePosition(nodeSpeed);
        }

        public int numEdges() {
            return numEdges;
        }

    }

    public void init(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();
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

        top = box + 1; // Storage For "Top" Is "Box" Plus One
        gl.glNewList(top, GL2.GL_COMPILE); // Now The "Top" Display List

        gl.glBegin(GL2.GL_QUADS);
        gl.glNormal3f(0.0f, 1.0f, 0.0f);
        //gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3f(-1.0f, 1.0f, -1.0f);// Top Face
        //gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3f(-1.0f, 1.0f, 1.0f);
        //gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3f(1.0f, 1.0f, 1.0f);
        //gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3f(1.0f, 1.0f, -1.0f);
        gl.glEnd();
        gl.glEndList();
    }


    float r0 = 0f;

    public void display(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();

        clear(gl);

        updateCamera(gl);

        List<ConceptsSource> s = this.sources;
        for (int i = 0, sourcesSize = s.size(); i < sourcesSize; i++) {
            render(gl, s.get(i));
        }

    }

    public void clear(GL2 gl) {
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
    }

    public void render(GL2 gl, ConceptsSource s) {

        @Deprecated float dt = Math.max(0.001f /* non-zero */, s.dt());

        List<VDraw> toDraw = s.visible;

        update(toDraw, dt);

        for (int i1 = 0, toDrawSize = toDraw.size(); i1 < toDrawSize; i1++) {

            gl.glPushMatrix();

            render(gl, dt, toDraw.get(i1));

            gl.glPopMatrix();
        }

        s.ready.set(true);
    }

    public void render(GL2 gl, float dt, VDraw v) {

        float[] pp = v.p;
        float x = pp[0], y = pp[1], z = pp[2];

        int n = v.numEdges();
        EDraw[] eee = v.edges;
        for (int en = 0; en < n; en++) {
            EDraw e = eee[en];
            VDraw ee = e.key;

            float[] eep = ee.p;
            gl.glColor4f(e.r, e.g, e.b, e.a);
            gl.glLineWidth(e.width);
            gl.glBegin(GL.GL_LINES);
            {
                gl.glVertex3f(x, y, z);
                gl.glVertex3f(eep[0], eep[1], eep[2]);
            }
            gl.glEnd();
        }


        //@Nullable Concept c = b.get();


        gl.glTranslatef(x, y, z);

        //gl.glRotatef(45.0f - (2.0f * yloop) + xrot, 1.0f, 0.0f, 0.0f);
        //gl.glRotatef(45.0f + yrot, 0.0f, 1.0f, 0.0f);


        float pri = v.pri;
        float p = pri * 0.75f + 0.25f;

        //Label
        //float lc = p*0.5f + 0.5f;

        float sc = 4f;
        gl.glScalef(sc * p, sc * p, sc * p);

        final float activationPeriods = 4f;
        gl.glColor4f(h(pri), 1f / (1f + (v.lag / (activationPeriods * dt))), h(v.budget.dur()), v.budget.qua() * 0.25f + 0.75f);
        gl.glCallList(box);

        gl.glColor4f(1f, 1f, 1f, 1f * p);
        float fontScale = 0.01f;

        gl.glScalef(fontScale, fontScale, 1f);
        float fontThick = 2f;
        gl.glLineWidth(fontThick);
        renderString(gl, GLUT.STROKE_ROMAN /*STROKE_MONO_ROMAN*/, v.label,
                0, 0, 1f); // Print GL Text To The Screen

        //n++;
    }

    public void update(List<VDraw> toDraw, float dt) {
        List<GraphLayout> ll = this.layout;
        for (int i1 = 0, layoutSize = ll.size(); i1 < layoutSize; i1++) {
            ll.get(i1).update(this, toDraw, dt);
        }
    }

    public void updateCamera(GL2 gl) {
        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -120f);
        //gl.glRotatef(r0,    1.0f, 0.0f, 0.0f);
        //gl.glRotatef(-r0/1.5f, 0.0f, 1.0f, 0.0f);
        gl.glRotatef(r0 / 2f, 0.0f, 0.0f, 1.0f);
        r0 += 0.3f;
    }

    public float h(float p) {
        return p * 0.9f + 0.1f;
    }

    public void reshape(GLAutoDrawable drawable,
                        int xstart,
                        int ystart,
                        int width,
                        int height) {
        GL2 gl = (GL2) drawable.getGL();

        height = (height == 0) ? 1 : height;

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        glu.gluPerspective(45, (float) width / height, 1, 500);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    public static class ConceptsSource {


        public final NAR nar;
        private final int capacity;


        private FasterList<VDraw> visible = new FasterList(0);


        public final MutableFloat maxPri = new MutableFloat(1.0f);
        public final MutableFloat minPri = new MutableFloat(0.0f);


        //private String keywordFilter;
        //private final ConceptFilter eachConcept = new ConceptFilter();

        final AtomicBoolean ready = new AtomicBoolean(false);
        private long now;
        private GraphWindow grapher;
        private float dt;

        public ConceptsSource(NAR nar, int maxNodes) {


            this.nar = nar;
            now = nar.time();

            this.capacity = maxNodes;

            nar.onFrame(nn -> {
                if (ready.get()) {
                    update();
                    ready.set(false);
                }
            });

//            includeString.addListener((e) -> {
//                //System.out.println(includeString.getValue());
//                //setUpdateable();
//            });
        }

        public void start(GraphWindow grapher) {
            this.grapher = grapher;
            ready.set(true);
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
            x.topWhile((BLink<Concept> b) -> {
                float pri = b.pri();
                if (pri != pri) {
                    //throw new RuntimeException("deleted item: " + b);
                    return true;
                }

                return v.add(grapher.update(visible.size(), b));

            }, capacity);

            GraphWindow g = grapher;
            for (int i1 = 0, toDrawSize = v.size(); i1 < toDrawSize; i1++) {
                g.post(now, v.get(i1));
            }
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
