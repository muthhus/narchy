package nars.gui;

import com.jogamp.newt.opengl.GLWindow;
import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.Term;
import nars.util.data.list.FasterList;
import nars.util.event.On;
import nars.util.experiment.DeductiveMeshTest;
import org.infinispan.util.function.TriConsumer;
import spacegraph.ListSpace;
import spacegraph.SpaceGraph;
import spacegraph.AbstractSpace;
import spacegraph.Spatial;
import spacegraph.layout.FastOrganicLayout;
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

        Default n = new Default(512, 4, 2, 2);
        //n.conceptActivation.setValue(0.5f);
        //n.nal(4);


        new DeductiveMeshTest(n, new int[]{4, 4}, 16384);
        //new ArithmeticInduction(n);

        newConceptWindow(n, 256, 32);

        n.loop(10f);

    }

    public static GLWindow newConceptWindow(Default n, int maxNodes, int maxEdges) {

        return new SpaceGraph<Term>(
                new NARSpace<Term, Spatial<Term>>(n, (nar, space, target) -> {
                    Bag<Concept> x = ((Default) nar).core.concepts;
                    x.topWhile(b -> {

                        final float initDistanceEpsilon = 5f;
                        final float initImpulseEpsilon = 25f;

                        ConceptWidget w = space.update(b.get().term(),
                                t -> new ConceptWidget(t, maxEdges, nar) {
                                    @Override
                                    public Dynamic newBody(CollisionShape shape, boolean collidesWithOthersLikeThis) {
                                        Dynamic x = super.newBody(shape, collidesWithOthersLikeThis);

                                        //impulse in a random direction
                                        x.impulse(v(SpaceGraph.r(initImpulseEpsilon),
                                                SpaceGraph.r(initImpulseEpsilon),
                                                SpaceGraph.r(initImpulseEpsilon)));

                                        return x;
                                    }
                                });

                        w.pri = b.priIfFiniteElseZero();



                        //place in a random direction
                        w.move(SpaceGraph.r(initDistanceEpsilon),
                                SpaceGraph.r(initDistanceEpsilon),
                                SpaceGraph.r(initDistanceEpsilon));


                        target.add(w);

                        return true;

                    }, maxNodes);

                }, maxNodes).with(
                        new Flatten(),
                        //new Spiral()
                        new FastOrganicLayout()
                )
        ).show(1300, 900);
    }


    private final int capacity;

    //public final MutableFloat maxPri = new MutableFloat(1.0f);
    //public final MutableFloat minPri = new MutableFloat(0.0f);


    //private String keywordFilter;
    //private final ConceptFilter eachConcept = new ConceptFilter();


    public NARSpace(TriConsumer<NAR, SpaceGraph<X>, List<Y>> collect, int capacity) {
        super();
        this.capacity = capacity;
        this.collect = collect;
    }

    public NARSpace(NAR nar, TriConsumer<NAR, SpaceGraph<X>, List<Y>> collect, int capacity) {
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
