package nars.util.jogl;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import nars.Global;
import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.util.AbstractJoglPanel;
import nars.util.data.list.FasterList;
import nars.util.data.list.LimitedFasterList;
import nars.util.event.Active;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.util.jogl.tutorial.Lesson14.renderString;

/**
 * Created by me on 6/20/16.
 */
public class JoglGraphPanel extends AbstractJoglPanel {




    public static void main(String[] args) {

        Default n = new Default(512,2,1,3);

        n.log();

        n.input("<a <-> b>. :|:");
        n.step();
        n.input("<b --> c>. :|:");
        n.step();
        n.input("<c --> d>. :|:");
        n.step();
        n.input("<d --> a>. :|:");
        n.step();
        n.input("<(a,b) <-> c>?");
        n.step();
        n.input("<(a,b,#x) <-> d>!");
        n.step();
        n.input("<(a,?x,d) <-> e>. :|:");
        n.step();
        n.input("wtf(1,2,#x)!");
        //n.input("<(c|a) --> (b&e)>! :|:");
        //n.step();
        //n.input("<x <-> a>! :|:");
        //n.run(5);



        new JoglGraphPanel(new ConceptsSource(n)).show(500, 500);
        n.loop(25f);

    }

    private static final GLU glu = new GLU();
    private static final GLUT glut = new GLUT();

    final FasterList<ConceptsSource> sources = new FasterList<>(1);

    private int box, top;

    public JoglGraphPanel(ConceptsSource c) {
        super();
        sources.add(c);
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
        gl.glEnable(GL2.GL_LIGHT0);

        gl.glEnable(GL2.GL_LIGHTING); // Enable Lighting

        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);


// Enable Material Coloring
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


//    private void update() {
//        if (decreaseX)
//            xrot -= 8f;
//        if (increaseX)
//            xrot += 8f;
//        if (decreaseY)
//            yrot -= 8f;
//        if (increaseY)
//            yrot += 8f;
//    }

    float r0 = 0f;

    public void display(GLAutoDrawable drawable) {
        GL2 gl = (GL2) drawable.getGL();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);


        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -90f);
        gl.glRotatef(r0,    1.0f, 0.0f, 0.0f);
        gl.glRotatef(r0/1.5f, 0.0f, 1.0f, 0.0f);
        gl.glRotatef(r0/3f, 0.0f, 0.0f, 1.0f);
        r0+=0.3f;

        gl.glLineWidth(1f);

        for (int i = 0, sourcesSize = sources.size(); i < sourcesSize; i++) {
            ConceptsSource s = sources.get(i);
//            if (s.ready.get())
//                continue; //not finished yet

            BLink<Concept>[] vv = s.vertices;
            if (vv != null) {
                long now = s.time();
                int n = 0;
                for (BLink<Concept> b : vv) {

                    float pri = b.pri();
                    if (pri!=pri) {
                        continue; //deleted
                    }

                    gl.glPushMatrix();



                    float lastForgetTime = b.getLastForgetTime();
                    if (lastForgetTime!=lastForgetTime)
                        lastForgetTime = now;

                    float timeSinceLastUpdate = now - lastForgetTime;
                    float act = 1f / (1f + (timeSinceLastUpdate/3f));

                    @Nullable Concept c = b.get();
                    //int hash = c.hashCode();

                    float ni = n / (float) Math.E;
                    final float bn = 1f;
                    float x = (float) Math.sin(ni) * (bn + ni);
                    float y = (float) Math.cos(ni) * (bn + ni);
                    float z = act*10f;

                    gl.glTranslatef(x, y, z);

                    //gl.glRotatef(45.0f - (2.0f * yloop) + xrot, 1.0f, 0.0f, 0.0f);
                    //gl.glRotatef(45.0f + yrot, 0.0f, 1.0f, 0.0f);


                    float p = pri /2f + 0.5f;
                    gl.glScalef(p, p, p);


                    gl.glColor4f(h(pri), act, h(b.dur()), b.qua()*0.25f + 0.75f);
                    gl.glCallList(box);

                    //Label
                    //float lc = p*0.5f + 0.5f;
                    gl.glColor4f(1f, 1f, 1f, 1f);
                    gl.glScalef(0.02f, 0.02f, 1f);
                    renderString(gl, GLUT.STROKE_ROMAN /*STROKE_MONO_ROMAN*/, c.toString(),
                            0, 0, -4f); // Print GL Text To The Screen


                    gl.glPopMatrix();

                    n++;
                }
            }
            s.ready.set(true);
        }

        //gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[0]);
//        int n = 8;
//        for (yloop = 1; yloop < n; yloop++) {
//            for (xloop = 0; xloop < yloop; xloop++) {
//                gl.glLoadIdentity();
//
//                gl.glTranslatef(1.4f + (xloop * 2.8f) -
//                        (yloop * 1.4f), ((6.0f - yloop) * 2.4f) - 7.0f, -20.0f);
//
//                gl.glRotatef(45.0f - (2.0f * yloop) + xrot, 1.0f, 0.0f, 0.0f);
//                gl.glRotatef(45.0f + yrot, 0.0f, 1.0f, 0.0f);
//
//                float s = (float)Math.random() + 0.5f;
//                gl.glScalef(s, s, s);
//
//                gl.glColor3fv(boxcol[(yloop - 1)%boxcol.length], 0);
//                gl.glCallList(box);
//
//                //gl.glColor3fv(topcol[yloop - 1], 0);
//                //gl.glCallList(top);
//            }
//        }
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

        final int maxNodes = 512;
        final int maxNodeLinks = 8; //per type

        public final MutableFloat maxPri = new MutableFloat(1.0f);
        public final MutableFloat minPri = new MutableFloat(0.0f);
        //public final SimpleStringProperty includeString = new SimpleStringProperty("");

//    private final BiFunction<TermNode, TermNode, TermEdge> edgeBuilder =
//            TLinkEdge::new;

        //private float _maxPri = 1f, _minPri;
        protected final FasterList<BLink<Concept>> concepts = new LimitedFasterList<>(maxNodes);
        //private String keywordFilter;
        //private final ConceptFilter eachConcept = new ConceptFilter();
        public BLink<Concept>[] vertices;
        final AtomicBoolean ready = new AtomicBoolean(true);

        public ConceptsSource(NAR nar) {

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
            Bag<Concept> x = ((Default) nar).core.concepts;
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


        protected final void commit(Collection<BLink<Concept>> ii) {
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
