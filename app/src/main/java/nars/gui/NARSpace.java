package nars.gui;

import jcog.event.On;
import jcog.list.FasterList;
import nars.gui.graph.EdgeDirected;
import nars.term.Term;
import nars.term.Termed;
import spacegraph.AbstractSpace;
import spacegraph.Active;
import spacegraph.SpaceGraph;
import spacegraph.Spatial;
import spacegraph.layout.Flatten;
import spacegraph.phys.util.Animated;
import spacegraph.space.ListSpace;

import java.util.Collection;
import java.util.List;


/**
 * thread-safe visualization of capacity-bound NAR data buffers
 * TODO extract to superclass: BufferedListSpace
 */
public abstract class NARSpace<X extends Termed, Y extends Spatial<X>> extends ListSpace<X, Y> implements Animated {


    //private final TriConsumer<NAR, SpaceGraph<Term>, List<Spatial<X>>> collect;
    private On on;

    public SpaceGraph<X> space;


    //public final MutableFloat maxPri = new MutableFloat(1.0f);
    //public final MutableFloat minPri = new MutableFloat(0.0f);


    //private String keywordFilter;
    //private final ConceptFilter eachConcept = new ConceptFilter();



    public NARSpace() {
        super();

//        nar.onCycle(x -> {
//            updateIfNotBusy(this::update);
//
//        });
    }

    @Override
    public boolean animate(float dt) {

        //updateIfNotBusy(this::render);
        render();

        return true;
    }

    @Override
    public synchronized void stop() {
        super.stop();
        //if (on!=null) {
            space.dyn.removeAnimation(this);
            active.forEach(space::remove);
            active.clear();
            on.off();
            on = null;
        //}
    }

    public final boolean running() {
        return on != null;
    }



    @Override
    public void start(SpaceGraph space) {
        this.space = space;
        space.dyn.addAnimation(this);
        //on = nar.onCycle(nn -> updateIfNotBusy(this::update));
    }



    /** swap buffers */
    protected void render() {

        List<Y> prev = this.active;
        prev.forEach(Active::deactivate);

        List<Y> next = new FasterList(prev.size() /* estimate */);
        get(next);

        //System.out.println(space.dyn.summary() + " " +  prev.size() + " prev " + next.size() + " next");

        this.active = next;

        prev.forEach(x -> {
            if (!x.preactive)
                x.order = -1;
        });
    }

    /** override to filter items */
    protected boolean include(X x) {
        return true;
    }

    abstract protected void get(Collection<Y> displayNext);



    /** displays in a window with default force-directed options */
    public SpaceGraph<Term> show(int w, int h, boolean flat) {
        AbstractSpace ss = flat ? with(new Flatten()) : this;

//                        new SpaceTransform<Term>() {
//                            @Override
//                            public void update(SpaceGraph<Term> g, AbstractSpace<Term, ?> src, float dt) {
//                                float cDepth = -9f;
//                                src.forEach(s -> {
//                                    ((SimpleSpatial)s).moveZ(
//                                            s.key.volume() * cDepth, 0.05f );
//                                });
//                            }
//                        }

                //new Flatten()
//                        new Flatten() {
//                            protected void locate(SimpleSpatial s, v3 f) {
//                                f.set(s.x(), s.y(), 10 - ((Term) (s.key)).volume() * 1);
//                            }
//                        }


                //new Spiral()
//                        //new FastOrganicLayout()

        SpaceGraph<Term> s = new SpaceGraph<>(ss);

        EdgeDirected fd = new EdgeDirected();
        s.dyn.addBroadConstraint(fd);
        s.camPos(0, 0, 90);

        //s.ortho(Vis.logConsole(nar, 90, 40, new FloatParam(0f)).opacity(0.25f));


        //Vis.conceptsWindow2D
        s
                //.add(new ZoomOrtho(logConsole(n, 120, 40, new FloatParam(0.25f)).opacity(0.5f)))
                //.ortho(logConsole( n, 90, 40, new FloatParam(0f)).opacity(0.25f))
                .camPos(0, 0, 90)
                .show(w, h);

        return s;

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

