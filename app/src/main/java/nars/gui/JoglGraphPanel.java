package nars.gui;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import nars.NAR;
import nars.bag.Bag;
import nars.bag.impl.ArrayBag;
import nars.budget.Budget;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.Default;
import nars.task.Task;
import nars.term.Termed;
import nars.util.AbstractJoglPanel;
import nars.util.data.Util;
import nars.util.data.list.FasterList;
import nars.util.data.list.LimitedFasterList;
import nars.util.event.Active;
import nars.util.experiment.DeductiveMeshTest;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.infinispan.commons.util.WeakValueHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static nars.gui.tutorial.Lesson14.renderString;

/**
 * Created by me on 6/20/16.
 */
public class JoglGraphPanel extends AbstractJoglPanel {


    private List<VDraw> toDraw = new FasterList();

    public static void main(String[] args) {

        Default n = new Default(1024,8,3,3);

        //n.log();

        new DeductiveMeshTest(n, new int[] { 6, 5 }, 16384);

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

        new JoglGraphPanel(new ConceptsSource(n, maxNodes)).show(500, 500);
        n.loop(25f);

    }

    private static final GLU glu = new GLU();
    private static final GLUT glut = new GLUT();

    final FasterList<ConceptsSource> sources = new FasterList<>(1);
    final WeakValueHashMap<Termed,VDraw> vdraw;

    int maxEdgesPerVertex = 16;

    float nodeSpeed = 0.02f;

    private int box, top;

    public JoglGraphPanel(ConceptsSource c) {
        super();

        vdraw = new WeakValueHashMap<>(1024);

        sources.add(c);
    }

    public VDraw update(BLink<Termed> t) {
        return pre(get(t.get()), t);
    }

    public VDraw get(Termed t) {
        return vdraw.computeIfAbsent(t, x -> new VDraw(x, maxEdgesPerVertex));
    }

    public VDraw getIfActive(Termed t) {
        VDraw v = vdraw.get(t);
        return v!=null && v.active() ? v : null;
    }

    /** get the latest info into the draw object */
    protected VDraw pre(VDraw v, BLink<Termed> b) {
        v.budget = b; //mark as active
        return v;
    }

    protected void post(float now, VDraw v) {
        Termed tt = v.key;

        Budget b = v.budget;
        v.pri = b.priIfFiniteElseZero();

        if (tt instanceof Concept) {
            Concept cc = (Concept)tt;

            float lastConceptForget = b.getLastForgetTime();
            if (lastConceptForget != lastConceptForget)
                lastConceptForget = now;

            @NotNull Bag<Termed> termlinks = cc.termlinks();
            @NotNull Bag<Task> tasklinks = cc.tasklinks();

            float lastTermlinkForget = ((BLink) (((ArrayBag) termlinks).get(0))).getLastForgetTime();
            if (lastTermlinkForget != lastTermlinkForget)
                lastTermlinkForget = lastConceptForget;

            v.lag = now - Math.max(lastConceptForget, lastTermlinkForget);
            //float act = 1f / (1f + (timeSinceLastUpdate/3f));

            v.clearEdges();
            int numEdges = v.edges.length;
            v.setLimit(numEdges /2);
            termlinks.topWhile(v::addTermLink);
            v.setLimit(numEdges /2);
            tasklinks.topWhile(v::addTaskLink);
            v.clearRemainingEdges();
        }


        layout(v);

        v.updatePosition(nodeSpeed/(1f+ v.pri));
        //v.updateEdges...
    }

    protected void layout(VDraw v) {
        //TODO abstract
        int hash = v.hash;
        //int vol = v.key.volume();

        //float ni = n / (float) Math.E;
        //final float bn = 1f;

        float baseRad = 20f;
        float rad = 35f;
        float p = v.pri;
        v.tp[0] = (float) Math.sin(hash/1024f) * (baseRad + rad * (p));
        v.tp[1] = (float) Math.cos(hash/1024f) * (baseRad + rad * (p));
        v.tp[2] =
                //1f/(1f+v.lag) * (baseRad/2f);
                v.budget.qua() * (baseRad + rad);
                //v.tp[2] = act*10f;


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


    /** vertex draw info */
    public class VDraw {
        public final nars.term.Termed key;
        private final int hash;
        private final EDraw[] edges;

        /** current x, y, z */
        public float p[] = new float[3];

        /** target x, y, z */
        public float tp[] = new float[3];

        public String label;

        public Budget budget;
        public float pri;

        /** measure of inactivity, in time units */
        public float lag;

        public VDraw(nars.term.Termed k, int edges) {
            this.key = k;
            this.label = k.toString();
            this.hash = k.hashCode();
            this.edges = new EDraw[edges];
            for (int i = 0; i < edges; i++)
                this.edges[i] = new EDraw();
        }

        @Override
        public boolean equals(Object obj) {
            return key.equals(((VDraw)obj).key);
        }

        @Override
        public int hashCode() {
            return hash;
        }


        /** nodeSpeed < 1.0 */
        public void updatePosition(float nodeSpeed) {
            float[] p = this.p;
            float[] tp = this.tp;
            for (int i = 0; i < 3; i++) {
                p[i] = Util.lerp(tp[i], p[i], nodeSpeed);
            }
        }

        transient int nextEdge = -1;
        transient int edgeLimit = -1;

        public void setLimit(int e) { this.edgeLimit = e; }

        public boolean addTermLink(BLink<Termed> ll) {
            return addEdge(ll, ll.get(), false) && (edgeLimit-- > 0);
        }
        public boolean addTaskLink(BLink<Task> ll) {
            if (ll == null)
                return true;
            @Nullable Task t = ll.get();
            if (t == null)
                return true;
            return addEdge(ll, t.term(), true) && (edgeLimit-- > 0);
        }

        public boolean addEdge(BLink l, Termed ll, boolean task) {

//            if (ll == null)
//                return false;

            float pri = l.pri();
            float dur = l.dur();
            float qua = l.qua();
            float baseLineWidth = 3f;

            float width = baseLineWidth * (1f + pri) * (1f + dur);
            int ne;
            EDraw[] ee = this.edges;
            float r, g, b;
            if (task) {
                r = pri;
                g = dur/2f;
                b = qua/2f;
            } else {
                b = pri;
                g = dur/2f;
                r = qua/2f;
            }
            VDraw target = getIfActive(ll);
            ee[ne = nextEdge++].set(target, width, r, g, b, dur * qua);
            return (ne+1< ee.length);

        }

        public void clearEdges() {
            nextEdge = 0;
        }

        public void clearRemainingEdges() {
            EDraw[] ee = this.edges;
            for (int i = nextEdge; i < ee.length; i++)
                ee[i].clear();
        }

        public boolean active() {
            return budget!=null;
        }
        public void inactivate() {
            budget = null;
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
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);


        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -120f);
        //gl.glRotatef(r0,    1.0f, 0.0f, 0.0f);
        //gl.glRotatef(-r0/1.5f, 0.0f, 1.0f, 0.0f);
        gl.glRotatef(r0/2f, 0.0f, 0.0f, 1.0f);
        r0+=0.3f;



        for (int i = 0, sourcesSize = sources.size(); i < sourcesSize; i++) {
            ConceptsSource s = sources.get(i);
//            if (s.ready.get())
//                continue; //not finished yet

            BLink<Termed>[] vv = s.vertices;
            if (vv != null) {
                float now = s.time();
                //int n = 0;

                List<VDraw> toDraw = this.toDraw;

                for (BLink<Termed> b : vv) {
                    float pri = b.pri();
                    if (pri!=pri) {
                        continue; //deleted
                    }

                    VDraw v = update(b);
                    toDraw.add(v);
                }

                for (VDraw v : toDraw) {

                    post(now, v);

                    gl.glPushMatrix();

                    float[] pp = v.p;
                    float x = pp[0], y = pp[1], z = pp[2];

                    for (EDraw e : v.edges) {
                        VDraw ee = e.key;
                        if (ee ==null)
                            break;
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

                    gl.glScalef(p, p, p);


                    gl.glColor4f(h(pri), 1f/(1f+v.lag), h(v.budget.dur()), v.budget.qua()*0.25f + 0.75f);
                    gl.glCallList(box);

                    gl.glColor4f(1f, 1f, 1f, 1f*p);
                    float fontScale = 0.01f;

                    gl.glScalef(fontScale, fontScale, 1f);
                    float fontThick = 2f;
                    gl.glLineWidth(fontThick);
                    renderString(gl, GLUT.STROKE_ROMAN /*STROKE_MONO_ROMAN*/, v.label,
                            0, 0, 1f); // Print GL Text To The Screen


                    gl.glPopMatrix();

                    //n++;
                }

                for (VDraw v : toDraw) {
                    v.inactivate();
                }
                toDraw.clear();
            }
            s.ready.set(true);
        }

    }

    public float h(float p) {
        return p*0.9f + 0.1f;
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

    public static class ConceptsSource  {


        public final NAR nar;
        private Active regs;


        public final MutableFloat maxPri = new MutableFloat(1.0f);
        public final MutableFloat minPri = new MutableFloat(0.0f);

        protected final FasterList<BLink<? extends Termed>> concepts;
        //private String keywordFilter;
        //private final ConceptFilter eachConcept = new ConceptFilter();
        public BLink<Termed>[] vertices;
        final AtomicBoolean ready = new AtomicBoolean(true);

        public ConceptsSource(NAR nar, int maxNodes) {

            concepts = new LimitedFasterList<>(maxNodes);
            this.nar = nar;

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

            //String _keywordFilter = includeString.get();
            //this.keywordFilter = _keywordFilter != null && _keywordFilter.isEmpty() ? null : _keywordFilter;

            //_minPri = this.minPri.floatValue();
            //_maxPri = this.maxPri.floatValue();

            //final int maxNodes = this.maxNodes;

            concepts.clear();
            Bag<? extends Termed> x = ((Default) nar).core.concepts;
            x.topWhile(concepts::add);
            commit(concepts);

//        Iterable<Termed> _concepts = StreamSupport.stream(x.spliterator(), false).filter(cc -> {
//
//            float p = getConceptPriority(cc);
//            if ((p < minPri) || (p > maxPri))
//                return false;
//
//
//            if (keywordFilter != null) {
//                if (cc.get().toString().contains(keywordFilter))
//                    return false;
//            }
//
//            return true;
//
//        }).collect(Collectors.toList());

        }


        protected final void commit(Collection<BLink<? extends Termed>> ii) {
            int iis = ii.size();
            if (this.vertices == null || this.vertices.length != iis)
                this.vertices = new BLink[iis];
            this.vertices = ii.toArray(this.vertices);
            ii.clear();
        }

        public long time() {
            return nar.time();
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
