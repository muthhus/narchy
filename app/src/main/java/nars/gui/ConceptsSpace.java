package nars.gui;

import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.Term;

import java.util.Collection;
import java.util.function.Function;

public class ConceptsSpace extends NARSpace<Term, ConceptWidget> {


    public final NAR nar;
    private final int maxNodes;
    private final int maxEdgesPerNode;

    public ConceptsSpace(NAR nar, int maxNodes, int maxEdgesPerNode) {
        super(nar);
        this.nar = nar;
        this.maxNodes = maxNodes;
        this.maxEdgesPerNode = maxEdgesPerNode;
    }

    @Override
    protected void get(Collection<ConceptWidget> displayNext) {
        Bag<Concept> x =
                //nar instanceof Default ?
                ((Default) nar).core.active;
        //((Default2)nar).active;

        //System.out.println(((Default) nar).core.concepts.size() + " "+ ((Default) nar).index.size());

        Function<Term,ConceptWidget> materializer = materializer();
        x.topWhile(b -> {

            //Concept Core
            Concept concept = b.get();
            Term ct = concept.term();
            if (!display(ct))
                return true;


            ConceptWidget root = space.getOrAdd(ct, materializer);
            root.concept = concept;

            //float bPri = root.pri = b.priIfFiniteElseZero();
            displayNext.add(root);
            return true;

        }, maxNodes);

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
