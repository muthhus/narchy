package nars.gui;

import com.jogamp.opengl.GL2;
import nars.Task;
import nars.bag.impl.ArrayBag;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.link.BLink;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.Cuboid;
import spacegraph.obj.EDraw;
import spacegraph.obj.widget.FloatSlider;
import spacegraph.obj.widget.Label;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.render.Draw;

import java.util.HashMap;
import java.util.function.Consumer;

import static spacegraph.math.v3.v;
import static spacegraph.obj.layout.Grid.col;
import static spacegraph.obj.layout.Grid.row;


public class ConceptWidget extends Cuboid<Term> implements Consumer<BLink<? extends Termed>> {

    public final ArrayBag<TermEdge> edges;
    private final ConceptsSpace space;

    public float pri;

    //caches a reference to the current concept
    public Concept concept;


    public ConceptWidget(Term x, ConceptsSpace space, int numEdges) {
        super(x,1, 1);


        this.pri = 0f;
        this.space = space;


        setFront(
            col(new Label(x.toString()),
                row(new FloatSlider( 0, 0, 4 ), new BeliefTableChart(space.nar, x))
                    //new CheckBox("?")
            )
        );

        //float edgeActivationRate = 1f;

//        edges = //new HijackBag<>(maxEdges * maxNodes, 4, BudgetMerge.plusBlend, nar.random);
        this.edges =
            new ArrayBag<>(numEdges, BudgetMerge.max, new HashMap<>(numEdges));
        edges.setCapacity(numEdges);

//        for (int i = 0; i < edges; i++)
//            this.edges.add(new EDraw());

    }


    @Override
    public Dynamic newBody(boolean collidesWithOthersLikeThis) {
        Dynamic x = super.newBody(collidesWithOthersLikeThis);


        //int zOffset = -10;
        final float initDistanceEpsilon = 100f;
        final float initImpulseEpsilon = 1f;

        //place in a random direction
        x.transform().set(SpaceGraph.r(initDistanceEpsilon),
                SpaceGraph.r(initDistanceEpsilon),
                SpaceGraph.r(initDistanceEpsilon));

        //impulse in a random direction
        x.impulse(v(SpaceGraph.r(initImpulseEpsilon),
                SpaceGraph.r(initImpulseEpsilon),
                SpaceGraph.r(initImpulseEpsilon)));

        x.setDamping(0.9f, 0.9f);
        return x;
    }

    @Override
    public void delete() {
        super.delete();
        if (concept!=null) {
            edges.clear();
            concept = null;
        }
    }


    @Nullable @Override public Surface onTouch(Collidable body, ClosestRay hitPoint, short[] buttons) {
        Surface s = super.onTouch(body, hitPoint, buttons);
        if (s!=null) {

        }
        return s;
    }

    public void commit() {

        edges.commit();
        edges.forEachKey(TermEdge::clear);

        Concept c = concept;

        //phase 1: collect
        c.tasklinks().forEach(this);
        c.termlinks().forEach(this);

        edges.forEach(ff -> ff.get().update(ff));

        Term tt = key;

        //Budget b = instance;

        float p = space.nar.activation(tt);
        p = (p == p) ? p : 0;// = 1; //pri = key.priIfFiniteElseZero();
        this.pri = p;


        float minSize = 0.1f;
        float maxSize = 16f;
        //sqrt because the area will be the sqr of this dimension
        float nodeScale = ((float) Math.sqrt(minSize + p * maxSize));//1f + 2f * p;
        //nodeScale /= Math.sqrt(tt.volume());
        scale(nodeScale, nodeScale, nodeScale);


        Draw.hsb((tt.op().ordinal() / 16f), 0.75f + 0.25f * p, 0.5f, 0.9f, shapeColor);


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
    public void renderAbsolute(GL2 gl) {
        renderEdges(gl);
    }


    void renderEdges(GL2 gl) {
        edges.forEach(ff -> {
            TermEdge f = ff.get();
            if (f.a > 0)
                render(gl, f);
        });
    }

    public void render(@NotNull GL2 gl, @NotNull EDraw e) {

        gl.glColor4f(e.r, e.g, e.b, e.a);

        float width = e.width;
        if (width <= 1f) {
            Draw.renderLineEdge(gl, this, e, 4f + width * 2f);
        } else {
            Draw.renderHalfTriEdge(gl, this, e, width * radius / 18f, e.r * 2f /* hack */);
        }
    }

    @Override
    public void accept(BLink<? extends Termed> tgt) {
        Termed ttt = tgt.get();
        Term tt = ttt.term();
        if (!tt.equals(key)) {
            ConceptWidget at = (ConceptWidget) space.space.getIfActive(tt);
            if (at!=null) {
                TermEdge ate = new TermEdge(at);

                BLink<TermEdge> x = edges.put(ate, tgt);
                if (x!=null) {
                    x.get().add(tgt, !(ttt instanceof Task));
                }
            }
        }
    }

    public static class TermEdge extends EDraw<Term,ConceptWidget> implements Termed {

        float termlinkPri, tasklinkPri;

        private final int hash;

        public TermEdge(@NotNull ConceptWidget target) {
            super(target);
            this.hash = target.key.hashCode();
        }

        protected void clear() {
            termlinkPri = tasklinkPri = 0;
        }

        public void add(BLink b, boolean termOrTask) {
            float p = b.pri();
            if (termOrTask) {
                termlinkPri += p;
            } else {
                tasklinkPri += p;
            }
        }

        @NotNull
        @Override
        public Term term() {
            return target.key;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || target.key.equals(o);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public void update(BLink<TermEdge> ff) {

            float priSum = (termlinkPri + tasklinkPri);

            if (priSum >= 0) {

                float priAvg = priSum/2f;

                float minLineWidth = 1f;
                float maxLineWidth = 5f;

                this.width = minLineWidth + (maxLineWidth - minLineWidth) * priSum;
                //z.r = 0.25f + 0.7f * (pri * 1f / ((Term)target.key).volume());
                this.r = ff.qua();
                this.g = 0.1f + 0.9f * (tasklinkPri);
                this.b = 0.1f + 0.9f * (termlinkPri);
                //this.a = 0.1f + 0.5f * pri;
                this.a = priAvg;
                //0.9f;

                this.attraction = priSum * 0.1f;
            } else {
                this.a = 0;
            }
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
