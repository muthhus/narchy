package nars.gui;

import com.google.common.collect.Iterables;
import jcog.bag.PLink;
import nars.NAR;
import nars.bag.Bagregate;
import nars.budget.BLink;
import nars.budget.RawBLink;
import nars.concept.Concept;
import nars.term.Term;

import java.util.Collection;
import java.util.function.Function;

public class ConceptsSpace extends NARSpace<Term, ConceptWidget> {


    public final NAR nar;
    private final int maxNodes;
    private final int maxEdgesPerNode;
    final Bagregate<Concept> bag;

    private static final com.google.common.base.Function<? super PLink<Concept>, ? extends BLink<Concept>> pLinkToBLinkHACK = (x) -> new RawBLink(x, x.pri(), 0.5f);

    public ConceptsSpace(NAR nar, int maxNodes, int maxEdgesPerNode) {
        super(nar);
        this.nar = nar;
        this.maxNodes = maxNodes;
        this.maxEdgesPerNode = maxEdgesPerNode;
        bag = new Bagregate<Concept>(Iterables.transform(nar.conceptsActive(), pLinkToBLinkHACK), maxNodes, 0.5f) {
            @Override
            protected boolean include(BLink<Concept> x) {
                return display(x.get().term());
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
