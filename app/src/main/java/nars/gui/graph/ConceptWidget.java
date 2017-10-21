package nars.gui.graph;

import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.impl.PLinkArrayBag;
import jcog.data.FloatParam;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import jcog.util.Flip;
import nars.Task;
import nars.concept.Concept;
import nars.gui.DynamicListSpace;
import nars.term.Term;
import nars.term.Termed;
import spacegraph.SpaceGraph;
import spacegraph.phys.Dynamic;
import spacegraph.phys.shape.SphereShape;
import spacegraph.render.Draw;
import spacegraph.space.EDraw;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static jcog.Util.sqr;
import static nars.gui.graph.ConceptWidget.ConceptVis2.TASKLINK;
import static nars.gui.graph.ConceptWidget.ConceptVis2.TERMLINK;
import static spacegraph.math.v3.v;


public class ConceptWidget extends TermWidget<Concept> {

    public float pri;

    private DynamicConceptSpace space;
    public final Flip<Map<Concept, ConceptEdge>> currentEdges = new Flip(LinkedHashMap::new);

    public ConceptWidget(Concept x) {
        super(x);


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
        return currentEdges.read().values();
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
    public void commit(TermVis termVis, DynamicListSpace space) {
        this.space = (DynamicConceptSpace) space;
        termVis.accept(this);
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

    static class EdgeComponent extends PLink<PriReference<? extends Termed>> {
        final ConceptWidget src, tgt;
        final int type;
        private final int hash;

        EdgeComponent(PriReference<? extends Termed> link, ConceptWidget src, ConceptWidget to, int type, float pri) {
            super(link, link.priElseZero());
            this.src = src;
            this.tgt = to;
            this.type = type;
            this.pri = pri;
            this.hash = Util.hashCombine(src.hash, to.hash, type);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (hash != o.hashCode()) return false;
            EdgeComponent that = (EdgeComponent) o;
            return type == that.type &&
                    src.equals(that.src) &&
                    tgt.equals(that.tgt);
        }

        @Override
        public boolean isDeleted() {
            if (!src.active() || !tgt.active()) {
                delete();
                return true;
            }
            return super.isDeleted();
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    public static class ConceptVis2 implements TermVis<ConceptWidget>, BiConsumer<ConceptWidget, PriReference<? extends Termed>> {

        static final int TASKLINK = 0;
        static final int TERMLINK = 1;


        public Bag<EdgeComponent, EdgeComponent> edges;

        int maxEdges;
        public final FloatParam minSize = new FloatParam(1f, 0.1f, 5f);
        public final FloatParam maxSizeMult = new FloatParam(2f, 1f, 5f);
        public final AtomicBoolean showLabel = new AtomicBoolean(true);
        public final FloatParam termlinkOpacity = new FloatParam(1f, 0f, 1f);
        public final FloatParam tasklinkOpacity = new FloatParam(1f, 0f, 1f);
        public final FloatParam lineWidthMax = new FloatParam(1f, 0f, 4f);
        public final FloatParam lineWidthMin = new FloatParam(0.1f, 0f, 4f);
        public final FloatParam separation = new FloatParam(1f, 0f, 6f);
        public final FloatParam lineAlphaMin = new FloatParam(0.1f, 0f, 1f);
        public final FloatParam lineAlphaMax = new FloatParam(0.8f, 0f, 1f);

        public ConceptVis2(int maxEdges) {
            super();
            this.maxEdges = maxEdges;

            this.edges =
                    //new PLinkHijackBag(0, 2);
                    new PLinkArrayBag<>(maxEdges,
                            //PriMerge.max,
                            //PriMerge.replace,
                            PriMerge.plus,
                            //new UnifiedMap()
                            //new LinkedHashMap()
                            new LinkedHashMap() //maybe: edgeBagSharedMap
                    );
        }

        @Override
        public void update(List<ConceptWidget> pending) {

            //float priSum = edges.priSum();

            pending.forEach(c -> {
                c.currentEdges.write().clear();
            });

            edges.commit(ee -> {
//                e.update(termlinkOpac, tasklinkOpac);
//                e.decay(0.95f);
//                e.priMult(0.95f);

                ConceptWidget src = ee.src;
                if (ee.tgt.active()) {
                    ConceptEdge ce = src.currentEdges.write().computeIfAbsent(ee.tgt.id, (t) ->
                            new ConceptEdge(src, ee.tgt, 0)
                    );
                    ce.merge(ee);
                }

            });
            float termlinkOpac = termlinkOpacity.floatValue();
            float tasklinkOpac = tasklinkOpacity.floatValue();
            float separation = this.separation.floatValue();
            float minLineWidth = this.lineWidthMin.floatValue();
            float MaxEdgeWidth = this.lineWidthMax.floatValue();
            float _lineAlphaMax = this.lineAlphaMax.floatValue();
            float _lineAlphaMin = this.lineAlphaMin.floatValue();
            float lineAlphaMin = Math.min(_lineAlphaMin, _lineAlphaMax);
            float lineAlphaMax = Math.max(lineAlphaMin, _lineAlphaMax);
            pending.forEach(c -> {
                float srcRad = c.radius();
                c.currentEdges.write().values().forEach(e -> {
                    //e.update(termlinkOpac, tasklinkOpac)

                    float edgeSum = (e.termlinkPri + e.tasklinkPri);

                    if (edgeSum >= 0) {


                        float p = e.priElseZero();
                        e.width = minLineWidth + 0.5f * sqr(1 + p * MaxEdgeWidth);

                        float taskish = e.tasklinkPri / edgeSum* termlinkOpac;
                        e.r = 0.05f + 0.65f * sqr(taskish);
                        float termish = e.termlinkPri / edgeSum* tasklinkOpac;
                        e.b = 0.05f + 0.65f * sqr(termish);
                        e.g = 0.1f * (1f - (e.r + e.g) / 1.5f);

                        e.a = Util.lerp(p * Math.max( taskish , termish  ), lineAlphaMin, lineAlphaMax);

                        //0.05f + 0.9f * Util.and(this.r * tasklinkBoost, this.g * termlinkBoost);

                        e.attraction = 0.5f * e.width / 2f;// + priSum * 0.75f;// * 0.5f + 0.5f;
                        float totalRad = srcRad + e.tgt().radius();
                        e.attractionDist =
                                //4f;
                                (totalRad * separation) + totalRad; //target.radius() * 2f;// 0.25f; //1f + 2 * ( (1f - (qEst)));
                    } else {
                        e.a = -1;
                        e.attraction = 0;
                    }
                });
                c.currentEdges.commit();
            });


        }

        @Override
        public void accept(ConceptWidget src, PriReference<? extends Termed> link) {
            float pri = link.priElseNeg1();
            if (pri < 0)
                return;

            Termed ttt = link.get();
            if (ttt == null)
                return;

            Term tt = ttt.term();
            if (!tt.equals(src.id.term())) {
                Concept cc = src.space.nar.concept(tt);
                if (cc != null) {
                    ConceptWidget tgt = src.space.space.get(cc);
                    if (tgt != null && tgt.active()) {
                        //                Concept c = space.nar.concept(tt);
                        //                if (c != null) {

                        int type;
                        if (!!(ttt instanceof Task)) {
                            type = TASKLINK;
                        } else {
                            type = TERMLINK;
                        }

                        edges.putAsync(new EdgeComponent(link, src, tgt, type, pri));
                        //new PLinkUntilDeleted(ate, pri)
                        //new PLink(ate, pri)

                        //                }
                    }
                }
            }
        }

        @Override
        public void accept(ConceptWidget cw) {
            float p = cw.pri;


            //long now = space.now();
//            float b = conceptWidget.concept.beliefs().eviSum(now);
//            float g = conceptWidget.concept.goals().eviSum(now);
//            float q = conceptWidget.concept.questions().priSum();

            //sqrt because the area will be the sqr of this dimension
            float ep = 1 + p;
            float minSize = this.minSize.floatValue();
            float nodeScale = minSize + (ep * ep) * maxSizeMult.floatValue();
            //1f + 2f * p;

            boolean atomic = (cw.id.op().atomic);
            if (atomic)
                nodeScale /= 2f;

            if (cw.shape instanceof SphereShape) {
                float r = nodeScale;
                cw.scale(r, r, r);
            } else {
                float l = nodeScale * 1.618f;
                float w = nodeScale;
                float h = 1; //nodeScale / (1.618f * 2);
                cw.scale(l, w, h);
            }


            if (cw.body != null) {
                float density = 5.5f;
                cw.body.setMass(nodeScale * nodeScale * nodeScale /* approx */ * density);
                cw.body.setDamping(0.99f, 0.9f);

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

            cw.currentEdges.write().clear();
            cw.id.tasklinks().forEach(x -> this.accept(cw, x));
            cw.id.termlinks().forEach(x -> this.accept(cw, x));


        }
    }

    public static class ConceptEdge extends EDraw<ConceptWidget> {

        float termlinkPri, tasklinkPri;

        public ConceptEdge(ConceptWidget src, ConceptWidget target, float pri) {
            super(src, target, pri);
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
        public boolean isDeleted() {
            boolean inactive = !id.getOne().active() || !id.getTwo().active();
            if (inactive) {
                delete();
                return true;
            }
            return super.isDeleted();
        }

        public void merge(EdgeComponent e) {
            float p = e.priElseZero();
            switch (e.type) {
                case TERMLINK:
                    this.termlinkPri += p;
                    break;
                case TASKLINK:
                    this.tasklinkPri += p;
                    break;
            }
            priMax(p);
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
