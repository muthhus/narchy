package nars.gui;

import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.Term;
import nars.util.experiment.DeductiveMeshTest;
import org.infinispan.util.function.TriConsumer;
import spacegraph.*;
import spacegraph.layout.FastOrganicLayout;
import spacegraph.layout.Flatten;

import java.util.List;


/** thread-safe visualization of capacity-bound NAR data buffers */
public class NARSpace<X, Y extends Spatial<X>> extends ListInput<X, Y>  {

    private final TriConsumer<NAR, SpaceGraph<X>, List<Y>> collect;

    private List<Y> next;

    public static void main(String[] args) {

        Default n = new Default(256, 4, 2, 2);
        n.conceptActivation.setValue(0.5f);
        //n.nal(4);


        new DeductiveMeshTest(n, new int[]{6,5}, 16384);
        //new ArithmeticInduction(n);

        final int maxNodes = 128;
        final int maxEdges = 16;

        new SpaceGraph<Term>(
                new NARSpace<>(n, (nar,space,target) -> {
                    Bag<Concept> x = ((Default) nar).core.concepts;
                    x.topWhile(b -> {

                        ConceptWidget w = space.update(b.get().term(),
                                t -> new ConceptWidget(t, maxEdges, nar));

                        w.pri = b.priIfFiniteElseZero();

                        target.add(w);

                        return true;

                    }, maxNodes);

                }, maxNodes)
        ).with(
                new Flatten()
                //new Spiral()
                //new FastOrganicLayout()
        ).show(1300, 900);

        n.loop(30f);

    }



    public final NAR nar;
    private final int capacity;

    //public final MutableFloat maxPri = new MutableFloat(1.0f);
    //public final MutableFloat minPri = new MutableFloat(0.0f);


    //private String keywordFilter;
    //private final ConceptFilter eachConcept = new ConceptFilter();


    public NARSpace(NAR nar, TriConsumer<NAR, SpaceGraph<X>, List<Y>> collect, int capacity) {
        this.nar = nar;
        this.capacity = capacity;
        this.collect = collect;
        nar.onFrame(nn -> updateIfNotBusy());
    }



    @Override
    public long now() {
        return nar.time();
    }

    @Override
    protected void updateImpl() {

        //String _keywordFilter = includeString.get();
        //this.keywordFilter = _keywordFilter != null && _keywordFilter.isEmpty() ? null : _keywordFilter;

        //_minPri = this.minPri.floatValue();
        //_maxPri = this.maxPri.floatValue();

        //final int maxNodes = this.maxNodes;


        List<Y> prev = active;

        List<Y> v = this.next = rewind(capacity);
        collect.accept(nar, space, v);

        //remove missing
        for (int i = 0, prevSize = prev.size(); i < prevSize; i++) {
            Spatial y = prev.get(i);
            if (!y.preactive) {
                y.stop(space);
            }
        }

        //System.out.println(capacity + " " + active.size() + " " + space.dyn.objects().size());

        this.active = next;

    }




}
