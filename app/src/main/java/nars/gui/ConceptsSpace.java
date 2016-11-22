package nars.gui;

import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.Term;

import java.util.Collection;

public class ConceptsSpace extends NARSpace<Term, ConceptWidget> {


    public final NAR nar;
    private final int maxNodes;
    private final int maxEdgesPerNode;

    public ConceptsSpace(NAR nar, int maxNodes, int maxEdgesPerNode) {
        super(nar, maxNodes);
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

        x.topWhile(b -> {

            //Concept Core
            Concept concept = b.get();
            Term ct = concept.term();
            if (!display(ct))
                return true;

            ConceptWidget root = space.update(ct, t -> new ConceptWidget(t, this, maxEdgesPerNode));
            root.concept = concept;

            //float bPri = root.pri = b.priIfFiniteElseZero();
            displayNext.add(root);
            return true;

        }, maxNodes);



    }

    @Override
    protected void update() {
        super.update();

        active.forEach(ConceptWidget::commit);
    }

}
