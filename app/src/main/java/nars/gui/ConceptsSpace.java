package nars.gui;

import com.google.common.collect.Iterables;
import nars.NAR;
import nars.bag.Bagregate;
import nars.budget.BLink;
import nars.concept.Concept;
import nars.term.Term;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConceptsSpace extends NARSpace<Term, ConceptWidget> {


    public final NAR nar;
    private final int maxNodes;
    private final int maxEdgesPerNode;
    final Bagregate<Concept> bag;

    public ConceptsSpace(NAR nar, int maxNodes, int maxEdgesPerNode) {
        super(nar);
        this.nar = nar;
        this.maxNodes = maxNodes;
        this.maxEdgesPerNode = maxEdgesPerNode;
        bag = new Bagregate<Concept>(Iterables.transform(nar.conceptsActive(), Supplier::get), maxNodes, 0.5f) {
            @Override
            protected boolean include(Concept x) {
                return display(x.term());
            }
        };
    }

    @Override
    protected void get(Collection<ConceptWidget> displayNext) {

        Function<Term,ConceptWidget> materializer = materializer();

        long now = nar.time();

        bag.forEach((BLink<Concept> b) ->{

            Concept concept = b.get();

            displayNext.add(
                space.getOrAdd(concept.term(), materializer).setConcept(concept, now)
            );

        });

    }

    private Function<Term, ConceptWidget> materializer() {
        return t -> new ConceptWidget(nar, t, maxEdgesPerNode);
    }

    @Override
    protected void update() {
        super.update();

        active.forEach(c -> c.commit(this));
    }

}
