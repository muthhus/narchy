package nars.gui;

import com.google.common.collect.Iterables;
import nars.NAR;
import nars.bag.Bagregate;
import nars.budget.BLink;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.Term;
import nars.test.DeductiveMeshTest;
import spacegraph.SpaceGraph;

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


    public static void main(String[] args) {

        Default n = new Default(128, 1, 1, 3);
        n.nal(1);

        new DeductiveMeshTest(n, new int[] {2, 2}, 16384);

        //Vis.conceptsWindow2D
        Vis.conceptsWindow3D
                (n, 128, 12).show(800, 600);

        SpaceGraph.window(new CycleView(n), 400, 400);
        //n.loop(1f);



        //n.log();
        //n.input("(a-->b).", "(b-->c).","(c-->d).");

        //new DeductiveChainTest(n, 10, 9999991, (x, y) -> $.p($.the(x), $.the(y)));

        //n.linkFeedbackRate.setValue(0.05f);


        //Param.DEBUG = true;
//        n
//                //.log()
//                //.logSummaryGT(System.out, 0.05f)
//                .input(
////                        "((parent($X,$Y) && parent($Y,$Z)) <=> grandparent($X,$Z)).",
////                        "parent(c, p).",
////                        "parent(p, g).",
////                        "grandparent(p, #g)?"
//                        "x:(a,(b,c))."
////                        "$0.9;0.9;0.9$ (a,(b,(c,(d,e))))."
////
//                );
//                //.run(800);
//


    }

}
