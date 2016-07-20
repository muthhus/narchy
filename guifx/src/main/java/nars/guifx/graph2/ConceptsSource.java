package nars.guifx.graph2;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import nars.$;
import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.guifx.graph2.source.SpaceGrapher;
import nars.link.BLink;
import nars.nar.Default;
import nars.term.Term;
import nars.term.Termed;
import nars.util.event.Active;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Example Concept supplier with some filters
 */
public class ConceptsSource extends GraphSource {


    public final NAR nar;
    private Active regs;

    final int maxNodes = 128;
    final int maxNodeLinks = 8; //per type

    public final SimpleDoubleProperty maxPri = new SimpleDoubleProperty(1.0);
    public final SimpleDoubleProperty minPri = new SimpleDoubleProperty(0.0);
    public final SimpleStringProperty includeString = new SimpleStringProperty("");

//    private final BiFunction<TermNode, TermNode, TermEdge> edgeBuilder =
//            TLinkEdge::new;

    private float _maxPri = 1f, _minPri;
    protected final List<Termed> concepts = $.newArrayList();
    private String keywordFilter;
    private final ConceptFilter eachConcept = new ConceptFilter();

    public ConceptsSource(NAR nar) {

        this.nar = nar;

        includeString.addListener((e) -> {
            //System.out.println(includeString.getValue());
            //setUpdateable();
        });
    }


    @Override
    public void updateEdge(TermEdge ee, Object link) {
        //rolling average
        /*ee.pri = lerp(
                ((BLink)link).pri(), ee.pri,
                      0.1f);*/

        ee.pri.addValue( ((BLink) link).pri() );
    }


    @Override
    public void updateNode(SpaceGrapher g, Termed s, TermNode sn) {
        sn.pri(nar.conceptPriority(s));
        super.updateNode(g, s, sn);
    }

    @Override
    public void forEachOutgoingEdgeOf(Termed cc,
                                      Consumer eachTarget) {


        SpaceGrapher sg = grapher;
//        if (sg == null)
//            throw new RuntimeException("grapher null");


        Term cct = cc.term();


        final int[] count = {0};
        final int[] max = {0};
        Predicate linkUpdater = link -> {

            Termed target = ((BLink<Termed>) link).get();

            //if (cct.equals(target)) //self-loop
            //    return true;

            TermNode tn = sg.getTermNode(target);
            if (tn != null) {
                eachTarget.accept(link); //tn.c);
                return (count[0]++) < max[0];
            }

            return true;

//            TermEdge.TLinkEdge ee = (TermEdge.TLinkEdge) getEdge(sg, sn, tn, edgeBuilder);
//
//            if (ee != null) {
//                ee.linkFrom(tn, link);
//            }
//
//            //missing.remove(tn.term);
        };

        max[0] = maxNodeLinks;
        ((Concept) cc).termlinks().topWhile(linkUpdater);
        max[0] = maxNodeLinks; //equal chance for both link types
        ((Concept) cc).tasklinks().topWhile(linkUpdater);

        //sn.removeEdges(missing);

    }

    @Override
    public Termed getTargetVertex(Termed edge) {

        return grapher.getTermNode(edge).c;
    }


    @Override
    public void start(SpaceGrapher g) {

        if (g != null) {

            //.stdout()
            //.stdoutTrace()
            //                .input("<a --> b>. %1.00;0.7%", //$0.9;0.75;0.2$
            //                        "<b --> c>. %1.00;0.7%")

            if (regs != null)
                throw new RuntimeException("already started");


            regs = new Active(
                    /*nar.memory.eventConceptActivated.on(
                            c -> refresh.set(true)
                    ),*/
                    nar.eventFrameStart.on(h -> {
                        //refresh.set(true);
                        updateGraph();
                    })
            );

            super.start(g);
        } else {
            if (regs == null)
                throw new RuntimeException("already stopped");

            regs.off();
            regs = null;
        }
    }


    @Override
    public void commit() {

        String _keywordFilter = includeString.get();
        this.keywordFilter = _keywordFilter != null && _keywordFilter.isEmpty() ? null : _keywordFilter;

        _minPri = this.minPri.floatValue();
        _maxPri = this.maxPri.floatValue();

        //final int maxNodes = this.maxNodes;

        //TODO use forEach witha predicate return to stop early
        eachConcept.reset();
        Bag<Concept> x = ((Default) nar).core.concepts;
        x.topWhile(eachConcept);

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

        commit(concepts);
    }


    protected final void commit(Collection<Termed> ii) {
        grapher.setVertices(ii);
        ii.clear();
    }

    private class ConceptFilter implements Predicate<BLink<Concept>> {

        int count;

        public void reset() {
            count = 0;
        }

        @Override
        public boolean test(BLink<Concept> cc) {


            float p = cc.pri();
            if ((p < _minPri) || (p > _maxPri)) {
                return true;
            }

            Concept c = cc.get();

            String keywordFilter1 = keywordFilter;
            if (keywordFilter1 != null) {
                if (!c.toString().contains(keywordFilter1)) {
                    return true;
                }
            }

            concepts.add(c);
            return count++ <= maxNodes;

        }
    }


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
