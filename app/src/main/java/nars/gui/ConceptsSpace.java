package nars.gui;

import nars.NAR;
import nars.bag.Bag;
import nars.bag.impl.experimental.HijackBag;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.Default;
import nars.term.Term;
import nars.term.Termed;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.List;
import java.util.function.Consumer;

public class ConceptsSpace extends NARSpace<Term, ConceptWidget> {

    final Bag<Pair<ConceptWidget, Term>> edges;

    private final NAR nar;
    private final int maxNodes;
    //private final int maxEdges;

    public ConceptsSpace(NAR nar, int maxNodes, int maxEdges) {
        super(nar, maxNodes);
        this.nar = nar;
        this.maxNodes = maxNodes;
        //this.maxEdges = maxEdges;

        edges = new HijackBag<>(maxEdges * maxNodes, 4, BudgetMerge.plusBlend, nar.random);
            //new CurveBag(BudgetMerge.plusBlend, nn.random);

    }

    @Override
    protected void get(List<ConceptWidget> displayNext) {
        Bag<Concept> x =
                //nar instanceof Default ?
                ((Default) nar).core.active;
        //((Default2)nar).active;

        //System.out.println(((Default) nar).core.concepts.size() + " "+ ((Default) nar).index.size());

        x.topWhile(b -> {

            //Concept Core
            Concept concept = b.get();
            if (!display(concept.term()))
                return true;

            ConceptWidget root = space.update(concept.term(), t -> new ConceptWidget(t, nar));

            //float bPri = root.pri = b.priIfFiniteElseZero();
            displayNext.add(root);


            Consumer<BLink<? extends Termed>> absorb = tgt -> {
                Term tt = tgt.get().term();
                if (!tt.equals(root.key)) {
                    edges.put(Tuples.pair(root, tt), tgt);
                }
            };

            //phase 1: collect
            root.clearEdges();
            concept.tasklinks().forEach(absorb);
            concept.termlinks().forEach(absorb);


            return true;

        }, maxNodes);


        //phase 2: add edges
        edges.forEach(eb -> {
            Pair<ConceptWidget, Term> ebt = eb.get();
            ebt.getOne().addEdge(space, ebt.getTwo(), eb);
        });
        edges.clear();


    }
}
