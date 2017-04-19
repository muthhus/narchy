package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.pri.PLink;
import jcog.pri.RawPLink;
import jcog.bag.impl.hijack.PLinkHijackBag;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.Dynamics;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.render.Draw;
import spacegraph.render.JoglPhysics;
import spacegraph.space.Cuboid;
import spacegraph.space.EDraw;

import java.util.function.Consumer;

import static jcog.Util.or;
import static spacegraph.math.v3.v;


public class ConceptWidget extends Cuboid<Term> implements Consumer<PLink<? extends Termed>> {

    public final PLinkHijackBag<TermEdge> edges;


    //caches a reference to the current concept
    public Concept concept;
    private final ConceptVis conceptVis = new ConceptVis2();
    private transient ConceptsSpace space;


    public ConceptWidget(NAR nar, Termed x, int numEdges) {
        super(x.term(), 1, 1);

        setFront(
//            /*col(
                //new Label(x.toString())
//                row(new FloatSlider( 0, 0, 4 ), new BeliefTableChart(nar, x))
//                    //new CheckBox("?")
//            )*/
                new ConceptIcon(nar, x)
        );

//        final PushButton icon = new PushButton(x.toString(), (z) -> {
//            setFront(new BeliefTableChart(nar, x).scale(4,4));
//        });
//        setFront(icon);

        //float edgeActivationRate = 1f;

//        edges = //new HijackBag<>(maxEdges * maxNodes, 4, BudgetMerge.plusBlend, nar.random);
        this.edges =
                new PLinkHijackBag(numEdges, 2, nar.random);
        //new ArrayBag<>(numEdges, BudgetMerge.avgBlend, new HashMap<>(numEdges));
        edges.setCapacity(numEdges);

//        for (int i = 0; i < edges; i++)
//            this.edges.add(new EDraw());

    }


    @Override
    public Dynamic newBody(boolean collidesWithOthersLikeThis) {
        Dynamic x = super.newBody(collidesWithOthersLikeThis);


        //int zOffset = -10;
        final float initDistanceEpsilon = 50f;
        final float initImpulseEpsilon = 0.25f;

        //place in a random direction
        x.transform().set(SpaceGraph.r(initDistanceEpsilon),
                SpaceGraph.r(initDistanceEpsilon),
                SpaceGraph.r(initDistanceEpsilon));

        //impulse in a random direction
        x.impulse(v(SpaceGraph.r(initImpulseEpsilon),
                SpaceGraph.r(initImpulseEpsilon),
                SpaceGraph.r(initImpulseEpsilon)));

        x.setDamping(0.25f, 0.85f);
        return x;
    }

    @Override
    public void delete(Dynamics dyn) {
        concept = null;
        super.delete(dyn);
        edges.clear();
    }


    @Override
    public Surface onTouch(Collidable body, ClosestRay hitPoint, short[] buttons, JoglPhysics space) {
        Surface s = super.onTouch(body, hitPoint, buttons, space);
        if (s != null) {

        }
        return s;
    }

    final static Truth zero = $.t(0.5f, 0.01f);

    public void commit(ConceptsSpace space) {

        this.space = space;


        Concept c = concept;

        if (c != null) {

            edges.commit(x -> {
                TermEdge xx = x.get();

                if (!xx.target.active()) {
                    x.delete();
                } else {
                    xx.clear();
                }

            });

            //phase 1: collect
            c.tasklinks().forEach(this);
            c.termlinks().forEach(this);

            float priSum = edges.priSum();
            edges.forEachKey(x -> x.update(priSum));

            conceptVis.apply(this, key);

        } else {
            edges.clear();
            delete(space.space.dyn);
        }

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

        float width = e.width;// * radius();
        if (width <= 0.1f) {
            Draw.renderLineEdge(gl, this, e, width);
        } else {
            Draw.renderHalfTriEdge(gl, this, e, width / 9f, e.r * 2f /* hack */);
        }
    }

    @Override
    public void accept(PLink<? extends Termed> tgt) {
        float pri = tgt.priSafe(-1);
        if (pri < 0)
            return;

        Termed ttt = tgt.get();
        Term tt = ttt.term();
        if (!tt.equals(key)) {
            Concept c = space.nar.concept(tt);
            if (c != null) {
                ConceptWidget at = space.widgetGet(c);
                if (at != null && at.active()) {
                    TermEdge ate = new TermEdge(at);
                    ate.add(tgt, !(ttt instanceof Task));
                    edges.put(new RawPLink(ate, pri));
                }
            }
        }
    }

//    public ConceptWidget setConcept(Concept concept, long when) {
//        this.concept = concept;
//        //icon.update(concept, when);
//        return this;
//    }

    public static class TermEdge extends EDraw<Term, ConceptWidget> implements Termed {

        float termlinkPri, tasklinkPri;

        private final int hash;

        public TermEdge(@NotNull ConceptWidget target) {
            super(target);
            this.hash = target.key.hashCode();
        }

        protected void clear() {
            termlinkPri = tasklinkPri = 0;
        }

        public void add(PLink b, boolean termOrTask) {
            float p = b.priSafe(0);
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

        public void update(float conceptEdgePriSum) {

            float edgeSum = (termlinkPri + tasklinkPri);

            if (edgeSum >= 0) {

                //float priAvg = priSum/2f;

                float minLineWidth = 10f;
                float priToWidth = 10f;

                this.width = minLineWidth + priToWidth * edgeSum;
                //z.r = 0.25f + 0.7f * (pri * 1f / ((Term)target.key).volume());
//                float qEst = ff.qua();
//                if (qEst!=qEst)
//                    qEst = 0f;


                if (edgeSum > 0) {
                    this.b = 0.1f;
                    this.r = 0.1f + 0.85f * (tasklinkPri / edgeSum);
                    this.g = 0.1f + 0.85f * (termlinkPri / edgeSum);
                } else {
                    this.r = this.g = this.b = 0.5f;
                }

                //this.a = 0.1f + 0.5f * pri;
                this.a = //0.1f + 0.5f * Math.max(tasklinkPri, termlinkPri);
                        //0.1f + 0.9f * ff.pri(); //0.9f;
                        0.1f + 0.9f * edgeSum/conceptEdgePriSum;

                this.attraction = 1f + 1f * edgeSum;// + priSum * 0.75f;// * 0.5f + 0.5f;
            } else {
                this.a = -1;
                this.attraction = 0;
            }

            this.attractionDist = 5f; //target.radius() * 2f;// 0.25f; //1f + 2 * ( (1f - (qEst)));
        }
    }

    public interface ConceptVis {
        void apply(ConceptWidget w, Term tt);
    }

    public static class ConceptVis1 implements ConceptVis {

        final float minSize = 0.1f;
        final float maxSize = 6f;


        @Override
        public void apply(ConceptWidget conceptWidget, Term tt) {
            float p = conceptWidget.space.nar.pri(tt, Float.NaN);
            p = (p == p) ? p : 0;// = 1; //pri = key.priIfFiniteElseZero();

            //sqrt because the area will be the sqr of this dimension
            float nodeScale = (float) (minSize + Math.sqrt(p) * maxSize);//1f + 2f * p;
            conceptWidget.scale(nodeScale, nodeScale, nodeScale);


            Draw.hsb((tt.op().ordinal() / 16f), 0.75f + 0.25f * p, 0.75f, 0.9f, conceptWidget.shapeColor);
        }
    }

    public static class ConceptVis2 implements ConceptVis {

        final float minSize = 2f;
        final float maxSize = 16f;

        @Override
        public void apply(ConceptWidget conceptWidget, Term tt) {
            ConceptsSpace space = conceptWidget.space;
            NAR nar = space.nar;
            float p = nar.pri(tt, 0);

            //long now = space.now();
//            float b = conceptWidget.concept.beliefs().eviSum(now);
//            float g = conceptWidget.concept.goals().eviSum(now);
//            float q = conceptWidget.concept.questions().priSum();
            float ec = p;// + w2c((b + g + q)/2f);

            //sqrt because the area will be the sqr of this dimension
            float nodeScale = (float) (minSize + Math.sqrt(ec) * maxSize);//1f + 2f * p;
            float l = nodeScale * 1.618f;
            float w = nodeScale;
            float h = nodeScale / (1.618f * 2);
            conceptWidget.scale(l, w, h);


            float density = 2f;
            if (conceptWidget.body != null)
                conceptWidget.body.setMass(l * w * h * density);

//            Draw.hsb(
//                    (tt.op().ordinal() / 16f),
//                    0.5f + 0.5f / tt.volume(),
//                    0.3f + 0.2f * p,
//                    0.9f, conceptWidget.shapeColor);

            Concept c = conceptWidget.concept;
            if (c != null) {
                Truth belief = c.belief(space.now, space.dur);
                if (belief == null) belief = zero;
                Truth goal = c.goal(space.now, space.dur);
                if (goal == null) goal = zero;

                float angle = 45 + belief.freq() * 180f + (goal.freq() - 0.5f) * 90f;
                Draw.hsb(angle / 360f, 0.5f, 0.25f + 0.5f * or(belief.conf(), goal.conf()), 0.9f, conceptWidget.shapeColor);
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
