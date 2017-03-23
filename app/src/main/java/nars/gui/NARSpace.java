package nars.gui;

import jcog.event.On;
import jcog.list.FasterList;
import nars.NAR;
import nars.nar.Default;
import nars.term.Term;
import nars.test.DeductiveMeshTest;
import spacegraph.Active;
import spacegraph.SpaceGraph;
import spacegraph.Spatial;
import spacegraph.space.ListSpace;

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
    protected boolean include(X x) {
        return true;
    }

    abstract protected void get(Collection<Y> displayNext);




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

