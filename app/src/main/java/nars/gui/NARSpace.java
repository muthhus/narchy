package nars.gui;

import com.jogamp.newt.opengl.GLWindow;
import nars.$;
import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.nar.Default;
import nars.op.ArithmeticInduction;
import nars.term.Term;
import nars.util.data.list.FasterList;
import nars.util.event.On;
import nars.util.experiment.DeductiveChainTest;
import nars.util.experiment.DeductiveMeshTest;
import org.infinispan.util.function.TriConsumer;
import org.jetbrains.annotations.Nullable;
import spacegraph.*;
import spacegraph.layout.Flatten;
import spacegraph.phys.Dynamic;
import spacegraph.phys.shape.CollisionShape;

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

        Default n = new Default(1024, 4, 2, 2);
        n.inputActivation.setValue(0.5f);
        //n.nal(4);


        new DeductiveMeshTest(n, new int[]{4, 4}, 16384);
        new DeductiveChainTest(n, 10, 9999991, (x, y) -> $.p($.the(x), $.the(y)));

        new ArithmeticInduction(n);

        newConceptWindow(n, 512, 8);

        //n.run(20); //headstart

        n.loop(25f);

    }

    public static GLWindow newConceptWindow(Default nn, int maxNodes, int maxEdges) {

        NARSpace<Term, Spatial<Term>> n = new NARSpace<>(nn, (nar, space, target) -> {
            Bag<Concept> x = ((Default) nar).core.concepts;

            //System.out.println(((Default) nar).core.concepts.size() + " "+ ((Default) nar).index.size());

            x.topWhile(b -> {

                final float initDistanceEpsilon = 10f;
                final float initImpulseEpsilon = 25f;

                ConceptWidget w = space.update(b.get().term(),
                        t -> new ConceptWidget(t, maxEdges, nar) {
                            @Override
                            public Dynamic newBody(boolean collidesWithOthersLikeThis) {
                                Dynamic x = super.newBody(collidesWithOthersLikeThis);

                                //place in a random direction
                                x.transform().set(SpaceGraph.r(initDistanceEpsilon),
                                        SpaceGraph.r(initDistanceEpsilon),
                                        SpaceGraph.r(initDistanceEpsilon));

                                //impulse in a random direction
                                x.impulse(v(SpaceGraph.r(initImpulseEpsilon),
                                        SpaceGraph.r(initImpulseEpsilon),
                                        SpaceGraph.r(initImpulseEpsilon)));

                                return x;
                            }
                        });

                w.pri = b.priIfFiniteElseZero();


                target.add(w);

                return true;

            }, maxNodes);

        }, maxNodes);


        SpaceGraph s = new SpaceGraph<Term>(
                n.with(
                        new SpaceTransform<Term>() {
                            @Override
                            public void update(SpaceGraph<Term> g, AbstractSpace<Term, ?> src, float dt) {
                                float cDepth = -9f;
                                src.forEach(s -> {
                                    ((SimpleSpatial)s).moveZ(
                                            s.key.volume() * cDepth, 0.05f );
                                });
                            }
                        }
                        //new Flatten()
                        //new Spiral()
                        //new FastOrganicLayout()
                )
        );

        s.dyn.addBroadConstraint(new SpaceGraph.ForceDirected());

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
