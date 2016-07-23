package nars.gui;

import com.jogamp.opengl.GL2;
import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.link.BLink;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import spacegraph.EDraw;
import spacegraph.SpaceGraph;
import spacegraph.Spatial;

import java.util.function.Predicate;


public class ConceptWidget extends Spatial<Term> {

    private final NAR nar;

    /**
     * measure of inactivity, in time units
     */
    //public float lag;

    public ConceptWidget(Term x, int maxEdges, NAR nar) {
        super(x, maxEdges);
        this.nar = nar;
    }

    @Override
    protected void renderRelativeAspect(GL2 gl) {
        gl.glColor4f(1f, 1f, 1f, pri);
        renderLabel(gl, 0.0005f);
    }

    @Override
    public void update(SpaceGraph<Term> s) {
        super.update(s);

        Term tt = key;

        //Budget b = instance;

        float p = pri;// = 1; //pri = key.priIfFiniteElseZero();

        float nodeScale = 1f + p * 3f;//1f + 2f * p;
        //nodeScale /= Math.sqrt(tt.volume());
        scale(nodeScale, nodeScale, nodeScale / 4f);


        Concept cc = nar.concept(tt);
        if (cc == null) {
            //remove? hide?
            return;
        }
//            float lastConceptForget = instance.getLastForgetTime();
//            if (lastConceptForget != lastConceptForget)
//                lastConceptForget = now;

        @NotNull Bag<Term> termlinks = cc.termlinks();
        @NotNull Bag<Task> tasklinks = cc.tasklinks();
//
//        if (!termlinks.isEmpty()) {
//            float lastTermlinkForget = ((BLink) (((ArrayBag) termlinks).get(0))).getLastForgetTime();
//            if (lastTermlinkForget != lastTermlinkForget)
//                lastTermlinkForget = lastConceptForget;
//        }

        //lag = now - Math.max(lastConceptForget, lastTermlinkForget);
        //lag = now - lastConceptForget;
        //float act = 1f / (1f + (timeSinceLastUpdate/3f));

        clearEdges();
        int maxEdges = edges.length;

        Predicate<BLink<? extends Termed>> linkAdder = l -> addLink(s, l);
        tasklinks.topWhile(linkAdder, maxEdges / 2);
        termlinks.topWhile(linkAdder, maxEdges - edgeCount()); //fill remaining edges

    }



    boolean addLink(SpaceGraph space, BLink<? extends Termed> ll) {

        Termed gg = ll.get();
        if (gg == null)
            return true;
        Spatial target = space.getIfActive(gg.term());
        if (target == null)
            return true;


        return addEdge(ll, target, gg instanceof Task);
    }


    public boolean addEdge(BLink l, Spatial target, boolean task) {

        EDraw[] ee = edges;

        float pri = l.pri();
        float dur = l.dur();
        float qua = l.qua();

        //width relative to the radius of the atom
        float minLineWidth = 0.02f;
        float maxLineWidth = 0.15f;
        float width = minLineWidth + (maxLineWidth - minLineWidth) * (1 + pri + (dur) * (qua));

        float r, g, b;
        float hp = 0.5f + 0.5f * pri;
        //float qh = 0.5f + 0.5f * qua;
        if (task) {
            Task x = (Task) l.get();
            if (x.isBeliefOrGoal()) {
                float e = x.expectation();
                if (e >= 0.5f) {
                    //positive in green
                    g = 0.5f + 0.5f * e;
                    r = 0;
                } else {
                    //negative in red
                    r = 0.5f + 0.5f * (1f-e);
                    g = 0;
                }
                b = 0.5f * dur;

            } else {
                //blue for questions and quests
                b = 0.5f + hp * qua;
                r = 0.2f;
                g = 0.2f;
            }
        } else {
            //gray for termlinks
            r = g = b = hp;
        }

        //int maxEdges = edges.length;
        float a = 0.5f;// / (maxEdges*0.5f /* est. avg 0.5 per link */);

        int n;
        ee[n = (numEdges++)].set(target, width,
                r, g, b, a
        );
        return (n - 1 <= ee.length);
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
