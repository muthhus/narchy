package nars.gui.graph;

import jcog.bag.util.Bagregate;
import jcog.pri.PriReference;
import nars.NAR;
import nars.control.ConceptFire;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class DynamicConceptSpace extends ConceptSpace {

    final Bagregate<ConceptFire> bag;
    private final int maxNodes;

    public DynamicConceptSpace(NAR nar, @NotNull Iterable<ConceptFire> concepts, int maxNodes, int bufferedNodes, int maxEdgesPerNodeMin, int maxEdgesPerNodeMax) {
        super(nar, maxEdgesPerNodeMin, maxEdgesPerNodeMax);

        this.maxNodes = maxNodes;

        final float bagUpdateRate = 0.5f;
        bag = new Bagregate<>(concepts, maxNodes + bufferedNodes, bagUpdateRate) {
            @Override
            protected boolean include(ConceptFire x) {
                return DynamicConceptSpace.this.include(x.term());
            }

            @Override
            public void onAdded(PriReference<ConceptFire> conceptPLink) {

            }

            @Override
            public void onRemoved(@NotNull PriReference<ConceptFire> value) {
                removeNode(value.get());
            }
        };
    }

      @Override
    protected void get(Collection<ConceptWidget> displayNext) {

        bag.update();
        bag.forEach(maxNodes, (PriReference<ConceptFire> concept) ->
            displayNext.add( nodeGetOrCreate(concept))
            //space.getOrAdd(concept.term(), materializer).setConcept(concept, now)
        );


//        //System.out.println(nar.time() + " " + displayNext.size() );
//        System.out.println("space:" + space.summary());
//        System.out.println("\twidgt:" + widgets.summary());
//        System.out.println("\tedges:" + edges.summary());
//        if (!displayNext.isEmpty())
//            System.out.println(displayNext.iterator().next());
    }
}
