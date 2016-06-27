package nars.gui;

import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.Default;
import nars.task.Task;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import spacegraph.EDraw;
import spacegraph.SpaceInput;
import spacegraph.Spatial;

import java.util.List;
import java.util.function.Predicate;

/**
 * Created by me on 6/26/16.
 */
public class ConceptBagInput extends SpaceInput<Termed, ConceptWidget> implements ConceptMaterializer {

    public final NAR nar;
    private final int capacity;
    private final int edgeCapacity;

    //public final MutableFloat maxPri = new MutableFloat(1.0f);
    //public final MutableFloat minPri = new MutableFloat(0.0f);


    //private String keywordFilter;
    //private final ConceptFilter eachConcept = new ConceptFilter();


    public ConceptBagInput(NAR nar, int maxNodes, int maxEdgesPerNode) {
        this.nar = nar;
        this.capacity = maxNodes;
        this.edgeCapacity = maxEdgesPerNode;
        nar.onFrame(nn -> updateIfNotBusy());
    }

    @Override
    public int numEdgesFor(Termed x) {
        return edgeCapacity;
    }


    @Override
    public float now() {
        return nar.time();
    }

    @Override
    protected void updateImpl() {

        //String _keywordFilter = includeString.get();
        //this.keywordFilter = _keywordFilter != null && _keywordFilter.isEmpty() ? null : _keywordFilter;

        //_minPri = this.minPri.floatValue();
        //_maxPri = this.maxPri.floatValue();

        //final int maxNodes = this.maxNodes;


        List<ConceptWidget> v = rewind(capacity);
        Bag<Concept> x = ((Default) nar).core.concepts;
        x.topWhile(this::accept, capacity);

        float now = now();
        for (int i1 = 0, toDrawSize = v.size(); i1 < toDrawSize; i1++) {
            update(now, v.get(i1));
        }
    }


    protected void update(float now, ConceptWidget v) {
        Termed tt = v.key;

        //Budget b = v.instance;
        float p = v.pri = 1; //v.pri = v.key.priIfFiniteElseZero();

        float nodeScale = 1f + 2f * p;
        nodeScale /= Math.sqrt(tt.volume());
        v.scale(nodeScale, nodeScale, nodeScale / 3f);

        if (tt instanceof Concept) {
            updateConcept(v, (Concept) tt, now);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void updateConcept(ConceptWidget v, Concept cc, float now) {

//            float lastConceptForget = v.instance.getLastForgetTime();
//            if (lastConceptForget != lastConceptForget)
//                lastConceptForget = now;

        @NotNull Bag<Termed> termlinks = cc.termlinks();
        @NotNull Bag<Task> tasklinks = cc.tasklinks();
//
//        if (!termlinks.isEmpty()) {
//            float lastTermlinkForget = ((BLink) (((ArrayBag) termlinks).get(0))).getLastForgetTime();
//            if (lastTermlinkForget != lastTermlinkForget)
//                lastTermlinkForget = lastConceptForget;
//        }

        //v.lag = now - Math.max(lastConceptForget, lastTermlinkForget);
        //v.lag = now - lastConceptForget;
        //float act = 1f / (1f + (timeSinceLastUpdate/3f));

        v.clearEdges();
        int maxEdges = v.edges.length;

        Predicate<BLink<? extends Termed>> linkAdder = l -> addLink(v, l);
        tasklinks.topWhile(linkAdder, maxEdges / 2);
        termlinks.topWhile(linkAdder, maxEdges - v.edgeCount()); //fill remaining edges

    }

    boolean addLink(ConceptWidget v, BLink<? extends Termed> ll) {

        Termed gg = ll.get();
        if (gg == null)
            return true;
        Spatial target = space.getIfActive(gg.term());
        if (target == null)
            return true;


        return addEdge(v, ll, target, gg instanceof Task);
    }


    public boolean addEdge(ConceptWidget v, BLink l, Spatial target, boolean task) {

        EDraw[] ee = v.edges;

        float pri = l.pri();
        float dur = l.dur();
        float qua = l.qua();

        //width relative to the radius of the atom
        float minLineWidth = 0.25f;
        float maxLineWidth = 0.85f;
        float width = minLineWidth + (maxLineWidth - minLineWidth) * (pri + (dur) * (qua));

        float r, g, b;
        float hp = 0.4f + 0.6f * pri;
        //float qh = 0.5f + 0.5f * qua;
        if (task) {
            r = hp;
            g = dur / 3f;
            b = 0;
        } else {
            b = hp;
            g = dur / 3f;
            r = 0;
        }
        float a = 0.25f + 0.75f * (pri);

        int n;
        ee[n = (v.numEdges++)].set(target, width,
                r, g, b, a
        );
        return (n - 1 <= ee.length);
    }


    public boolean accept(BLink<Concept> b) {

        float pri = b.pri();
        if (pri != pri) {
            //throw new RuntimeException("deleted item: " + b);
            return true;
        }

        int nextID = this.visible.size();
        ConceptWidget w = (ConceptWidget) space.update(nextID, this, b.get());
        return this.visible.add(w);
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
