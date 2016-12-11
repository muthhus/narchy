package nars.gui;

import nars.NAR;
import nars.nar.Default;
import nars.term.Term;
import nars.test.DeductiveMeshTest;
import nars.util.event.On;
import nars.util.list.FasterList;
import spacegraph.Active;
import spacegraph.SpaceGraph;
import spacegraph.Spatial;
import spacegraph.source.ListSpace;

import java.util.Collection;
import java.util.LinkedHashSet;


/**
 * thread-safe visualization of capacity-bound NAR data buffers
 */
public abstract class NARSpace<X extends Term, Y extends Spatial<X>> extends ListSpace<X, Y> {


    //private final TriConsumer<NAR, SpaceGraph<Term>, List<Spatial<X>>> collect;
    private On on;

    private NAR nar;
    protected SpaceGraph<X> space;


    final Collection<Y> next;

    //public final MutableFloat maxPri = new MutableFloat(1.0f);
    //public final MutableFloat minPri = new MutableFloat(0.0f);


    //private String keywordFilter;
    //private final ConceptFilter eachConcept = new ConceptFilter();



    public NARSpace(NAR nar) {
        super();
        this.next =
                new LinkedHashSet<>();
        this.nar = nar;
    }


    @Override
    public final void stop() {
        if (on!=null) {
            next.forEach(Active::hide);
            next.clear();
            on.off();
            on = null;
        }
    }

    public final boolean running() {
        return on != null;
    }


    @Override
    public long now() {
        return nar.time();
    }

    @Override
    public void start(SpaceGraph space) {
        this.space = space;
        on = nar.onCycle(nn -> updateIfNotBusy(this::update));
    }

    protected void update() {

        Collection<Y> prev = active;

        prev.forEach(Active::deactivate);

        get(next);

        //commit the changes
        this.active = new FasterList<>(next);
        next.clear();
    }

    /** override to filter items */
    protected boolean display(X x) {
        return true;
    }

    abstract protected void get(Collection<Y> displayNext);


    public static void main(String[] args) {

        Default n = new Default(512, 4, 1, 3);
        //Default2 n = new Default2();
        //n.nal(4);

        //n.DEFAULT_BELIEF_PRIORITY = 0.1f;
        //n.activationGlobal.setValue(0.5f);

        //new ArithmeticInduction(n);

        Vis.conceptsWindow3D(n, 64, 12).show(800, 600);

        //n.run(20); //headstart



        //n.log();
        //n.input("(a-->b).", "(b-->c).","(c-->d).");
        new DeductiveMeshTest(n, new int[] {4, 4}, 16384);

        //new DeductiveChainTest(n, 10, 9999991, (x, y) -> $.p($.the(x), $.the(y)));

        //n.linkFeedbackRate.setValue(0.05f);


        //Param.DEBUG = true;
//        n
//                //.log()
//                //.logSummaryGT(System.out, 0.05f)
//                .input(
////                        "((parent($X,$Y) && parent($Y,$Z)) <=> grandparent($X,$Z)).",
////                        "parent(c, p).",
////                        "parent(p, g).",
////                        "grandparent(p, #g)?"
//                        "x:(a,(b,c))."
////                        "$0.9;0.9;0.9$ (a,(b,(c,(d,e))))."
////
//                );
//                //.run(800);
//

        n.loop(16f);

    }


}

//    public static ConceptWidget newLinkWidget(final NAR nar, SpaceGraph<Term> space, final ConceptWidget core, Term SRC, Term TARGET, BLink bt, boolean task) {
//
//
//
//        @NotNull Compound vTerm = $.p(L, SRC, TARGET);
//        SimpleSpatial targetSpatial = (SimpleSpatial) space.getIfActive(TARGET);
//        if (targetSpatial!=null) {
//            ConceptWidget termLink = space.update(vTerm,
//                    t -> new ConceptWidget(t, nar) {
//
//                        //                                @Override
////                                public Dynamic newBody(boolean collidesWithOthersLikeThis) {
////                                    shape = new SphereShape(.5f);
////                                    Dynamic bb = super.newBody(collidesWithOthersLikeThis);
////                                    return bb;
////                                }
//
//
//                        @Override
//                        protected String label(Term term) {
//                            return "";
//                        }
//
//                        @Override
//                        public void update(SpaceGraph<Term> s) {
//                            super.update(s);
//
//                            clearEdges();
//
//
//                            EDraw in = addEdge(bt, core, task);
//                            in.attraction = 0.25f;
//
//
//                            EDraw out = addEdge(bt, targetSpatial, task);
//                            out.attraction = 1f + (0.5f * bt.priIfFiniteElseZero());
//
//
//                        }
//                    });
//            if (termLink!=null) {
//                termLink.pri = bt.priIfFiniteElseZero();
//            }
//            return termLink;
//        }
//
//        return null;
//
//    }

