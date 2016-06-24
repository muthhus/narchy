package nars.gui.graph;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.ui.JoglPhysics;
import com.google.common.collect.Lists;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.gl2.GLUT;
import nars.Global;
import nars.NAR;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.concept.Concept;
import nars.gui.graph.layout.FastOrganicLayout;
import nars.link.BLink;
import nars.nar.Default;
import nars.task.Task;
import nars.term.Termed;
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
public class GraphSpace<X extends VDraw> extends JoglPhysics<X> {


    public static void main(String[] args) {

        Default n = new Default(1024, 8, 6, 8);
        n.nal(4);


        new DeductiveMeshTest(n, new int[]{5,5}, 16384);

        final int maxNodes = 64;

        new GraphSpace(new ConceptsSource(n, maxNodes)).show(900, 900);
        n.loop(15f);

    }


    //private final GleemControl gleem = new GleemControl();

    final FasterList<ConceptsSource> sources = new FasterList<>(1);
    final WeakValueHashMap<Termed, VDraw> vdraw;

    int maxEdgesPerVertex = 9;

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
        return vdraw.computeIfAbsent(t, x -> new VDraw(this, x, maxEdgesPerVertex));
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
        v.activate((short)i, b);
        return v;
    }

    protected void post(float now, VDraw v) {
        Termed tt = v.key;

        Budget b = v.budget;
        float p = v.pri = b.priIfFiniteElseZero();

        float nodeScale = 1f + 2f * p;
        nodeScale /= Math.sqrt(tt.volume());
        v.scale(nodeScale, nodeScale, nodeScale/3f);

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


    static float r(float range) {
        return (-0.5f + (float)Math.random()*range)*2f;
    }


    public void init(GL2 gl) {
        super.init(gl);

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
            gl.glVertex3f(0, h,  0f); //right base
            gl.glVertex3f(0, -h, 0f); //left base
            gl.glVertex3f(1,  0, 0f);  //midpoint on opposite end

            gl.glEnd();
            gl.glEndList();
        }
    }



    public synchronized void display(GLAutoDrawable drawable) {

        List<CollisionObject<X>> undyn = Global.newArrayList();
        dyn.getCollisionObjectArray().stream().forEach(c -> {
            X vd = c.getUserPointer();
            if (!vd.active()) {
                undyn.add(c);
                c.setUserPointer(null); //remove reference so vd can be GC
                vd.body = null;
            }
        });
        undyn.forEach(dyn::removeCollisionObject);
        undyn.clear();

        super.display(drawable);
        //GL2 gl = (GL2) drawable.getGL();

        //clear(gl);

        //updateCamera(gl);

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

        s.busy.set(true);

        update(toDraw, dt);

        for (int i1 = 0, toDrawSize = toDraw.size(); i1 < toDrawSize; i1++) {

            render(gl, dt, toDraw.get(i1));

        }

        s.busy.set(false);
    }

    public void render(GL2 gl, float dt, VDraw v) {

        gl.glPushMatrix();

        gl.glTranslatef(v.x(), v.y(), v.z());

        v.render(gl, dt);

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


        float r = v.radius;
        gl.glScalef(r, r, r);

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
        float r = v.radius;
        renderString(gl, GLUT.STROKE_ROMAN /*STROKE_MONO_ROMAN*/, v.label,
                div * r, //scale
                0, 0, (r/1.9f)/div); // Print GL Text To The Screen


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

    public void renderHalfTriEdge(GL2 gl, VDraw src, EDraw e, float width) {
        VDraw tgt = e.key;


        gl.glPushMatrix();

        {

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

            gl.glCallList(isoTri);
        }

        gl.glPopMatrix();

    }

    public static void renderLineEdge(GL2 gl, VDraw src, EDraw e, float width) {
        VDraw tgt = e.key;
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
