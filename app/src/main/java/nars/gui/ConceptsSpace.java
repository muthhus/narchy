package nars.gui;

import com.google.common.collect.Iterables;
import nars.NAR;
import nars.bag.Bagregate;
import nars.budget.BLink;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.Term;
import nars.test.DeductiveChainTest;
import spacegraph.SpaceGraph;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.gui.Vis.*;
import static nars.gui.Vis.reflect;
import static nars.gui.Vis.stack;
import static nars.test.DeductiveChainTest.inh;

public class ConceptsSpace extends NARSpace<Term, ConceptWidget> {


    public static final float UPDATE_RATE = 0.5f;
    public final NAR nar;
    private final int maxEdgesPerNode;
    final Bagregate<Concept> bag;

    public ConceptsSpace(NAR nar, int maxNodes, int maxEdgesPerNode) {
        super(nar);
        this.nar = nar;
        this.maxEdgesPerNode = maxEdgesPerNode;
        bag = new Bagregate<Concept>(Iterables.transform(nar.conceptsActive(), Supplier::get), maxNodes, UPDATE_RATE) {
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

        Default n = new Default(64, 1, 1, 3);
        n.nal(1);
        n.DEFAULT_BELIEF_PRIORITY = 0.05f;
        n.DEFAULT_QUESTION_PRIORITY = 1f;

//        n.inputAt(1, "c:a?");
//        n.inputAt(2, "b:a.");
//        n.inputAt(3, "c:b.");

        new DeductiveChainTest(n, 8,  2048, inh);

        //new DeductiveMeshTest(n, new int[] {3, 3}, 16384);

        //Vis.conceptsWindow2D
        conceptsWindow3D(n, 64, 8)
                .camPos(0, 0, 90)
                //.ortho( logConsole(n, 40, 10, 0.0f) )
                .show(1300, 900);

        SpaceGraph.window(
            stack(
                reflect( new CycleView(n) )
                //logConsole(n, 120, 40, 0.25f).
            ),
        400, 400);

        n.loop(3f);



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
