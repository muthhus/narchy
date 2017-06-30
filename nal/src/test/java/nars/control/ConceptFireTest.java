package nars.control;

import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.concept.Concept;
import nars.nar.NARBuilder;
import nars.task.DerivedTask;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.function.Consumer;

import static nars.$.$;

public class ConceptFireTest {

    @Test
    public void testConceptFireLinkSelection() throws Narsese.NarseseException {
        NAR nar = new NARBuilder().get();
        nar.believe("a:b");
        nar.run(1);
        Concept c = nar.concept("a:b");
        for (int n = 0; n < 9; n++) {
            c.termlinks().put(new PLink<Term>($(n + ":a"), 0.1f * n));
        }
        c.print();

        ConceptFire cf = new ConceptFire(c, 1f) {
            @Override
            protected int run(NAR nar, @Nullable PriReference<Task> tasklink, @Nullable PriReference<Term> termlink, Consumer<DerivedTask> x, int ttlPerPremise) {
                System.out.println("tasklink=" + tasklink + " termlink=" + termlink);
                return super.run(nar, tasklink, termlink, x, ttlPerPremise);
            }
        };
        cf.run(nar);

    }

}