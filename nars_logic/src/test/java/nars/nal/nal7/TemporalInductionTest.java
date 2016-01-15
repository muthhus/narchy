package nars.nal.nal7;

import nars.NAR;
import nars.concept.Concept;
import nars.nar.Default;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 6/8/15.
 */
public class TemporalInductionTest {

    @Test
    public void testTemporalInduction() {

        String task = "<a --> b>. :|:";
        String task2 = "<c --> d>. :|:";

        NAR n = new Default();

        //TextOutput.out(n);

        n.input(task);
        n.frame(10);
        n.input(task2);

        n.frame(10);

    }

    @Test public void testTemporalRevision() {

        NAR n = new Default();

        //TextOutput.out(n);

        n.input("a:b. %1.0|0.9%");
        n.frame(5);
        n.input("a:b. %0.0|0.9%");
        n.frame(1);

        //n.forEachConcept(Concept::print);

        Concept c = n.concept("a:b");
        assertEquals("(b-->a). 5+0 %.50;.95%", c.getBeliefs().top(n.time()).toStringWithoutBudget());
    }

    @Test public void testTemporalRevisionOfTemporalRelation() {

        NAR n = new Default();

        //TextOutput.out(n);

        n.input("(a ==>+0 b). %1.0;0.7%");
        n.input("(a ==>+5 b). %1.0;0.6%");
        n.frame(1);

        n.forEachConcept(Concept::print);

        //Concept c = n.concept("a:b");
        //assertEquals("(b-->a). 5+0 %.50;.95%", c.getBeliefs().top().toStringWithoutBudget());
    }
    @Test public void testQuestionProjection() {

        NAR n = new Default();

        n.log();

        n.input("a:b. :|:");
        //n.frame();
        n.input("a:b? :/:");
        n.frame(5);
        n.input("a:b? :/:");
        n.frame(30);
        n.input("a:b? :/:");
        n.frame(250);
        n.input("a:b? :/:");
        n.frame(1);

        //n.forEachConcept(Concept::print);

        //Concept c = n.concept("a:b");
        //assertEquals("(b-->a). 5+0 %.50;.95%", c.getBeliefs().top().toStringWithoutBudget());
    }

}
