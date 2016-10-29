package nars.gui;

import com.jogamp.opengl.GL2;
import nars.$;
import nars.NAR;
import nars.budget.Budget;
import nars.link.BLink;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.EDraw;
import spacegraph.SimpleSpatial;
import spacegraph.SpaceGraph;
import spacegraph.phys.Dynamic;
import spacegraph.render.Draw;

import java.util.List;

import static nars.nal.UtilityFunctions.or;
import static nars.util.Util.sqr;
import static spacegraph.math.v3.v;


public class ConceptWidget extends SimpleSpatial<Term> {

    private final NAR nar;


    /** cached .toString() of the key */
    public String label;

    public float pri;

    public List<EDraw> edges;




    public ConceptWidget(Term x, NAR nar) {
        super(x);
        this.nar = nar;

        this.pri = 0.5f;
        edges = $.newArrayList(0);
//        for (int i = 0; i < edges; i++)
//            this.edges.add(new EDraw());

    }



    @Override
    public Dynamic newBody(boolean collidesWithOthersLikeThis) {
        Dynamic x = super.newBody(collidesWithOthersLikeThis);



        int zOffset = -10;
        final float initDistanceEpsilon = 10f;
        final float initImpulseEpsilon = 25f;

        //place in a random direction
        x.transform().set(SpaceGraph.r(initDistanceEpsilon),
                SpaceGraph.r(initDistanceEpsilon),
                SpaceGraph.r(initDistanceEpsilon) + zOffset);

        //impulse in a random direction
        x.impulse(v(SpaceGraph.r(initImpulseEpsilon),
                SpaceGraph.r(initImpulseEpsilon),
                SpaceGraph.r(initImpulseEpsilon)));

        return x;
    }

    public void clearEdges() {
        if (!edges.isEmpty())
            this.edges = $.newArrayList(8);
    }

    public int edgeCount() {
        return edges.size();
    }


    @Override
    protected void renderRelativeAspect(GL2 gl) {
        renderLabel(gl, 0.05f);
    }


    @Override
    public void update(@NotNull SpaceGraph<Term> s) {
        super.update(s);

        Term tt = key;

        //Budget b = instance;

        this.pri = nar.activation(tt);

        float p = pri;// = 1; //pri = key.priIfFiniteElseZero();

        float nodeScale = ((1+p)*(1+p)) * 2f;//1f + 2f * p;
        //nodeScale /= Math.sqrt(tt.volume());
        scale(nodeScale, nodeScale, nodeScale / 4f);


        Draw.hsb( (tt.op().ordinal()/16f), 0.75f + 0.25f * p, 0.5f  , 0.9f, shapeColor);



//            float lastConceptForget = instance.getLastForgetTime();
//            if (lastConceptForget != lastConceptForget)
//                lastConceptForget = now;

//        @NotNull Bag<Term> termlinks = cc.termlinks();
//        @NotNull Bag<Task> tasklinks = cc.tasklinks();

//        if (!termlinks.isEmpty()) {
//            float lastTermlinkForget = ((BLink) (((ArrayBag) termlinks).get(0))).getLastForgetTime();
//            if (lastTermlinkForget != lastTermlinkForget)
//                lastTermlinkForget = lastConceptForget;
//        }

        //lag = now - Math.max(lastConceptForget, lastTermlinkForget);
        //lag = now - lastConceptForget;
        //float act = 1f / (1f + (timeSinceLastUpdate/3f));

//        Concept cc = nar.concept(tt);
//        if (cc != null) {
//            //remove? hide?
//    //        clearEdges();
//            int maxEdges = edges.length;
//    //
//            Consumer<BLink<? extends Termed>> linkAdder = l -> {
//                if (numEdges < maxEdges)
//                    addLink(s, l);
//            };
//            @NotNull Bag<Task> tasklinks = cc.tasklinks();
//            tasklinks.forEach(linkAdder);
//        }
        //tasklinks.topWhile(linkAdder, maxEdges - edgeCount()); //fill remaining edges
//        termlinks.topWhile(linkAdder, maxEdges - edgeCount()); //fill remaining edges

    }




    boolean addEdge(SpaceGraph space, BLink<? extends Termed> ll) {

        Termed gg = ll.get();
        if (gg == null)
            return true;
        return addEdge(space, gg, ll)!=null;
    }

    @Nullable
    public EDraw addEdge(@NotNull SpaceGraph space, @NotNull Termed gg, @NotNull  Budget ll) {
        SimpleSpatial target = (SimpleSpatial) space.getIfActive(gg.term());
        if (target == null)
            return null;

        return addEdge(ll, target);
    }


    @Nullable public EDraw addEdge(@NotNull Budget l, @NotNull SimpleSpatial target) {

        List<EDraw> ee = edges;

        float maxAttraction = 0.2f;

        float pri = l.priIfFiniteElseZero();
        if (pri > 0) {

            float minLineWidth = 1f;
            float maxLineWidth = 5f;
            float width = minLineWidth + (maxLineWidth - minLineWidth) * pri;

            EDraw z = new EDraw();
            z.target = target;
            z.width = width;
            z.r = 0.25f + 0.7f * (pri * 1f / ((Term)target.key).volume());
            float qua = l.qua();
            z.g = 0.25f + 0.7f * (pri * qua);
            float dur = l.dur();
            z.b = 0.25f + 0.7f * (pri * dur);
            z.a = 0.5f + 0.5f * pri;
                    //0.9f;
            z.attraction = sqr(or(qua,dur))*maxAttraction;

            ee.add(z);
            return z;
        } else {
            return null;
        }
    }

    @Override
    protected void renderAbsolute(GL2 gl) {
        renderEdges(gl);
    }


    void renderEdges(GL2 gl) {
        List<EDraw> eee = edges;
        int n = eee.size();
        for (int en = 0; en < n; en++) {
            EDraw f = eee.get(en);
            if (f!=null)
                render(gl, f);
        }
    }

    public void render(@NotNull GL2 gl, @NotNull EDraw e) {

        gl.glColor4f(e.r, e.g, e.b, e.a);

        float width = e.width;
        if (width <= 1f) {
            Draw.renderLineEdge(gl, this, e, 4f+width*2f);
        } else {
            Draw.renderHalfTriEdge(gl, this, e, width*radius/18f, e.r*2f /* hack */);
        }
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
