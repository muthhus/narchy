package nars.gui.graph;

import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.impl.PLinkArrayBag;
import jcog.data.FloatParam;
import jcog.pri.Deleteable;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.Task;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import org.eclipse.collections.impl.tuple.Tuples;
import spacegraph.SpaceGraph;
import spacegraph.phys.Dynamic;
import spacegraph.render.Draw;
import spacegraph.space.EDraw;

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static jcog.Util.sqr;
import static spacegraph.math.v3.v;


public class ConceptWidget extends TermWidget<Concept> implements Consumer<PriReference<? extends Termed>> {

    public float pri;

    public Bag<ConceptEdge, ConceptEdge> edges;
    private TermSpace<Concept> space;

    public static final Function<Concept, ConceptWidget> nodeBuilder = ConceptWidget::new;

    public ConceptWidget(Concept x) {
        super(x);

        this.edges =
                //new PLinkHijackBag(0, 2);
                new PLinkArrayBag<>(0,
                        //PriMerge.max,
                        //PriMerge.replace,
                        PriMerge.plus,
                        //new UnifiedMap()
                        //new LinkedHashMap()
                        new LinkedHashMap() //maybe: edgeBagSharedMap
                );

//        final PushButton icon = new PushButton(x.toString(), (z) -> {
//            setFront(new BeliefTableChart(nar, x).scale(4,4));
//        });
//        setFront(icon);

        //float edgeActivationRate = 1f;

//        edges = //new HijackBag<>(maxEdges * maxNodes, 4, BudgetMerge.plusBlend, nar.random);


//        for (int i = 0; i < edges; i++)
//            this.edges.add(new EDraw());

    }

    @Override
    public Iterable<ConceptEdge> edges() {
        return ()->edges.iterator();
    }


    @Override
    public Dynamic newBody(boolean collidesWithOthersLikeThis) {
        Dynamic x = super.newBody(collidesWithOthersLikeThis);

        final float initDistanceEpsilon = 50f;

        //place in a random direction
        x.transform().set(
                SpaceGraph.r(initDistanceEpsilon),
                SpaceGraph.r(initDistanceEpsilon),
                SpaceGraph.r(initDistanceEpsilon));

        //impulse in a random direction
        final float initImpulseEpsilon = 0.25f;
        x.impulse(v(
                SpaceGraph.r(initImpulseEpsilon),
                SpaceGraph.r(initImpulseEpsilon),
                SpaceGraph.r(initImpulseEpsilon)));

        return x;
    }

    //final RecycledSummaryStatistics edgeStats = new RecycledSummaryStatistics();

    @Override
    public void commit(TermVis termVis, TermSpace space) {

        this.space = space;

//        Concept c = id;
//
//        if (c == null || c.isDeleted())
//            return;



        //int newCap = Util.lerp(pri, space.maxEdgesPerNodeMin, space.maxEdgesPerNodeMax);
        edges.commit();

        //phase 1: collect
        //edgeStats.clear();

        id.tasklinks().forEach(this);
        //float tasklinkVar = (float) edgeStats.getStandardDeviation()/2f;
        //if (tasklinkVar != tasklinkVar) tasklinkVar = 0; //none anyway

        //edgeStats.clear();
        id.termlinks().forEach(this);
        //float termlinkVar = (float) edgeStats.getStandardDeviation()/2f;
        //if (termlinkVar != termlinkVar) termlinkVar = 0; //none anyway

        float priSum = edges.priSum();


//        public final FloatParam termlinkOpacity = new FloatParam(1f, 0f, 1f);
//        public final FloatParam tasklinkOpacity = new FloatParam(1f, 0f, 1f);

        float termlinkOpac = 1; //termVis.termlinkOpacity.floatValue();
        float tasklinkOpac = 1; //termVis.tasklinkOpacity.floatValue();

//                float p = edges.depressurize();
//                float decayRate =
        edges.commit(e -> {
            e.update(ConceptWidget.this, priSum, termlinkOpac, tasklinkOpac);
            e.decay(0.95f);
        });

        termVis.accept(this);


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


    //    @Override
//    protected void renderRelativeAspect(GL2 gl) {
//        renderLabel(gl, 0.05f);
//    }


//    @Nullable
//    public EDraw addEdge(@NotNull Budget l, @NotNull ConceptWidget target) {
//
//        EDraw z = new TermEdge(target, l);
//        edges.add(z);
//        return z;
//    }


    @Override
    public void accept(PriReference<? extends Termed> tgt) {
        float pri = tgt.priElseNeg1();
        if (pri < 0)
            return;

        Termed ttt = tgt.get();
        if (ttt == null)
            return;

        Term tt = ttt.term();
        if (!tt.equals(id)) {
            Concept cc = ((DynamicConceptSpace) space).nar.concept(tt);
            if (cc!=null) {
                ConceptWidget to = space.space.get(cc);
                if (to != null && to.active()) {
                    //                Concept c = space.nar.concept(tt);
                    //                if (c != null) {
                    ConceptEdge ate =
                            space.edges.apply(Tuples.pair(cc, to));
                    ate.priAdd(pri);
                    ate.add(pri, !(ttt instanceof Task));
                    edges.putAsync(ate);
                    //new PLinkUntilDeleted(ate, pri)
                    //new PLink(ate, pri)

                    //                }
                }
            }
        }
    }


//    public ConceptWidget setConcept(Concept concept, long when) {
//        this.concept = concept;
//        //icon.update(concept, when);
//        return this;
//    }

    public static class ConceptVis1 implements TermVis<ConceptWidget> {

        final float minSize = 0.1f;
        final float maxSize = 6f;


        @Override
        public void accept(ConceptWidget cw) {
            float p = cw.pri;
            p = (p == p) ? p : 0;// = 1; //pri = key.priIfFiniteElseZero();

            //sqrt because the area will be the sqr of this dimension
            float nodeScale = (float) (minSize + Math.sqrt(p) * maxSize);//1f + 2f * p;
            cw.scale(nodeScale, nodeScale, nodeScale);


            Draw.hsb((cw.id.op().ordinal() / 16f), 0.75f + 0.25f * p, 0.75f, 0.9f, cw.shapeColor);
        }


    }

    public static class ConceptVis2 implements TermVis<ConceptWidget> {

        int maxEdgesPerNode;
        public final FloatParam minSize = new FloatParam(2f, 0.1f, 5f);
        public final FloatParam maxSizeMult = new FloatParam(4f, 1f, 5f);
        public final AtomicBoolean showLabel = new AtomicBoolean(true);

        public ConceptVis2(int maxEdgesPerNodeMax) {
            super();
            this.maxEdgesPerNode = maxEdgesPerNodeMax;
        }


        @Override
        public void accept(ConceptWidget cw) {
            float p = cw.pri;

            cw.edges.setCapacity(maxEdgesPerNode);

            //long now = space.now();
//            float b = conceptWidget.concept.beliefs().eviSum(now);
//            float g = conceptWidget.concept.goals().eviSum(now);
//            float q = conceptWidget.concept.questions().priSum();

            //sqrt because the area will be the sqr of this dimension
            float ep = 1 + p;
            float minSize = this.minSize.floatValue();
            float nodeScale = minSize + (ep * ep * ep) * maxSizeMult.floatValue();
            //1f + 2f * p;

            boolean atomic = (cw.id.op().atomic);
            if (atomic)
                nodeScale/=2f;

            float l = nodeScale * 1.618f;
            float w = nodeScale;
            float h = 1; //nodeScale / (1.618f * 2);
            cw.scale(l, w, h);


            if (cw.body != null) {
                float density = 10.5f;
                cw.body.setMass(l * w * h * density);
                cw.body.setDamping(0.9f, 0.9f);
            }

//            Draw.hsb(
//                    (tt.op().ordinal() / 16f),
//                    0.5f + 0.5f / tt.volume(),
//                    0.3f + 0.2f * p,
//                    0.9f, conceptWidget.shapeColor);

            if (!showLabel.get())
                cw.front.hide();
            else
                cw.front.scale(1f, 1f);

//            Concept c = cw.id;
//            if (c != null) {
////                Truth belief = c.belief(space.now, space.dur);
////                if (belief == null) belief = zero;
////                Truth goal = c.goal(space.now, space.dur);
////                if (goal == null) goal = zero;
////
////                float angle = 45 + belief.freq() * 180f + (goal.freq() - 0.5f) * 90f;
//                //angle / 360f
                Draw.colorHash(cw.id, cw.shapeColor);// * or(belief.conf(), goal.conf()), 0.9f, cw.shapeColor);
//            }

        }
    }

    public static class ConceptEdge extends EDraw<TermWidget<Term>> implements Termed {

        float termlinkPri, tasklinkPri;

        private final int hash;

        public ConceptEdge(TermWidget target) {
            super(target);
            this.hash = target.id.hashCode();
        }


        protected void decay(float rate) {
            //termlinkPri = tasklinkPri = 0;

            //decay
            termlinkPri *= rate;
            tasklinkPri *= rate;
        }


        public void add(float p, boolean termOrTask) {
            if (termOrTask) {
                termlinkPri += p;
            } else {
                tasklinkPri += p;
            }
        }

        @Override
        public Term term() {
            return id.id.id;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || id.id.equals(o);
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        public void update(ConceptWidget src, float conceptEdgePriSum, float termlinkBoost, float tasklinkBoost) {

            float edgeSum = (termlinkPri + tasklinkPri);


            if (edgeSum >= 0) {

                //float priAvg = priSum/2f;

                float minLineWidth = 0.2f;

                final float MaxEdgeWidth = 4;

                this.width = minLineWidth + sqr(1 + pri * MaxEdgeWidth);

                //z.r = 0.25f + 0.7f * (pri * 1f / ((Term)target.key).volume());
//                float qEst = ff.qua();
//                if (qEst!=qEst)
//                    qEst = 0f;


                this.r = 0.05f + 0.65f * sqr(tasklinkPri / edgeSum);
                this.b = 0.05f + 0.65f * sqr(termlinkPri / edgeSum);
                this.g = 0.1f * (1f - (r + g) / 1.5f);


                this.a = 0.5f;
                        //0.05f + 0.9f * Util.and(this.r * tasklinkBoost, this.g * termlinkBoost);

                this.attraction = 0.01f * width;// + priSum * 0.75f;// * 0.5f + 0.5f;
                this.attractionDist = 1f + 2 * src.radius() + id.radius(); //target.radius() * 2f;// 0.25f; //1f + 2 * ( (1f - (qEst)));
            } else {
                this.a = -1;
                this.attraction = 0;
            }

        }

        @Override
        public boolean isDeleted() {
            return super.isDeleted() || id.active();
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
