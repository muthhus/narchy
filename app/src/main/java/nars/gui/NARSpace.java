package nars.gui;

import com.jogamp.newt.opengl.GLWindow;
import nars.$;
import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import nars.util.data.list.FasterList;
import nars.util.event.On;
import nars.util.experiment.DeductiveMeshTest;
import org.infinispan.util.function.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.*;
import spacegraph.layout.Flatten;
import spacegraph.phys.Dynamic;
import spacegraph.phys.shape.SphereShape;

import java.util.List;

import static spacegraph.math.v3.v;


/**
 * thread-safe visualization of capacity-bound NAR data buffers
 */
public class NARSpace<X, Y extends Spatial<X>> extends ListSpace<X, Y> {

    private final TriConsumer<NAR, SpaceGraph<X>, List<Y>> collect;
    private On on;

    private NAR nar;

    public static void main(String[] args) {

        Default n = new Default(256, 2, 3, 4 );
        //n.nal(4);



        //new ArithmeticInduction(n);

        newConceptWindow(n, 256, 0);

        //n.run(20); //headstart

        n.loop(55f);

        //n.log();
        //n.input("(a<->b).", "(b<->c).");

        new DeductiveMeshTest(n, new int[]{4, 4}, 16384);
        //new DeductiveChainTest(n, 10, 9999991, (x, y) -> $.p($.the(x), $.the(y)));

    }

    public static GLWindow newConceptWindow(Default nn, int maxNodes, int maxEdges) {

        NARSpace<Term, Spatial<Term>> n = new NARSpace<>(nn, (nar, space, target) -> {
            Bag<Concept> x = ((Default) nar).core.concepts;

            //System.out.println(((Default) nar).core.concepts.size() + " "+ ((Default) nar).index.size());


            x.topWhile(b -> {

                //Concept Core
                Concept concept = b.get();

                ConceptWidget core = space.update(concept.term(),
                        t -> new ConceptWidget(t, maxEdges, nar));

                core.pri = b.priIfFiniteElseZero();
                target.add(core);


                concept.termlinks().forEach(bt -> {

                    Term tlSrc = concept.term();
                    final Term tlTarget = bt.get();
                    if (tlTarget.equals(tlSrc))
                        return; //no self loop

                    SimpleSpatial targetSpatial = (SimpleSpatial) space.getIfActive(tlTarget);
                    if (targetSpatial==null)
                        return;

                    @NotNull Compound vTerm = $.p($.the("termlink"), $.p(tlSrc, tlTarget));

                    ConceptWidget termLink = space.update(vTerm,
                            t -> new ConceptWidget(t, 2, nar) {

                                @Override
                                public Dynamic newBody(boolean collidesWithOthersLikeThis) {
                                    shape = new SphereShape(.5f);
                                    Dynamic bb = super.newBody(collidesWithOthersLikeThis);
                                    return bb;
                                }

                                @Override
                                public void update(SpaceGraph<Term> s) {
                                    super.update(s);

                                    clearEdges();


                                        EDraw in = addEdge(bt, core, false);
                                        in.attraction = 2f;

                                        EDraw out = addEdge(bt, targetSpatial, false);
                                        //out.attraction = 1f;


                                    //nothing
                                }
                            });

                    termLink.pri = bt.priIfFiniteElseZero();

                    target.add(termLink);
                });

                return true;

            }, maxNodes);

        }, maxNodes);


        SpaceGraph s = new SpaceGraph<>(

                n.with(
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
//                        //new Spiral()
//                        //new FastOrganicLayout()
                )
        );

        s.dyn.addBroadConstraint(new ForceDirected());



        return s.show(1300, 900);
    }


    private final int capacity;

    //public final MutableFloat maxPri = new MutableFloat(1.0f);
    //public final MutableFloat minPri = new MutableFloat(0.0f);


    //private String keywordFilter;
    //private final ConceptFilter eachConcept = new ConceptFilter();


    public NARSpace(@Nullable TriConsumer<NAR, SpaceGraph<X>, List<Y>> collect, int capacity) {
        super();
        this.capacity = capacity;
        this.collect = collect == null ? (TriConsumer<NAR, SpaceGraph<X>, List<Y>>) this : collect;
    }

    public NARSpace(NAR nar, @Nullable TriConsumer<NAR, SpaceGraph<X>, List<Y>> collect, int capacity) {
        this(collect, capacity);
        start(nar);
    }

    public final synchronized void start(NAR nar) {
        if (on != null)
            throw new RuntimeException("already running");
        this.nar = nar;
        on = nar.onFrame(nn -> updateIfNotBusy(this::update));
    }

    public final synchronized void stop() {
        on.off();
        on = null;
    }

    public final boolean running() {
        return on != null;
    }


    @Override
    public long now() {
        return nar.time();
    }



    protected void update(AbstractSpace _notused) {

        this.space = _notused.space;

        List<Y> prev = active;

        prev.forEach((y) -> y.preactivate(false));

        FasterList<Y> next = new FasterList<>(capacity);

        //gather the items, preactivating them
        collect.accept(nar, space, next);

        //remove missing
        for (int i = 0, prevSize = prev.size(); i < prevSize; i++) {
            Spatial y = prev.get(i);
            if (!y.preactive) {
                y.stop();
            }
        }

        //commit the changes
        this.active = next;
    }


}
