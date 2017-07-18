package nars.gui.graph;

import jcog.bag.util.Bagregate;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.control.Activate;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class DynamicConceptSpace extends ConceptSpace {

    final Bagregate<Activate> bag;
    private final int maxNodes;
    private final List next;
    final float bagUpdateRate = 0.1f;

    public DynamicConceptSpace(NAR nar, @NotNull Iterable<Activate> concepts, int maxNodes, int bufferedNodes, int maxEdgesPerNodeMin, int maxEdgesPerNodeMax) {
        super(nar, maxEdgesPerNodeMin, maxEdgesPerNodeMax);

        this.maxNodes = maxNodes;

        bag = new Bagregate<>(concepts, maxNodes + bufferedNodes, bagUpdateRate) {
            @Override
            protected boolean include(Activate x) {
                return DynamicConceptSpace.this.include(x.term());
            }

            @Override
            public void onAdded(PriReference<Activate> conceptPLink) {

            }

            @Override
            public void onRemoved(@NotNull PriReference<Activate> value) {
                removeNode(value.get());
            }
        };

        next = $.newArrayList();
        nar.onCycle(nn -> {
            bag.update();
            next.clear();
            bag.forEach(maxNodes, (PriReference<Activate> concept) ->
                            next.add(nodeGetOrCreate(concept))
                    //space.getOrAdd(concept.term(), materializer).setConcept(concept, now)
            );
            update();
        });
    }

    @Override
    protected void get(Collection<ConceptWidget> displayNext) {

        displayNext.addAll(next);

//        //System.out.println(nar.time() + " " + displayNext.size() );
//        System.out.println("space:" + space.summary());
//        System.out.println("\twidgt:" + widgets.summary());
//        System.out.println("\tedges:" + edges.summary());
//        if (!displayNext.isEmpty())
//            System.out.println(displayNext.iterator().next());
    }
}
