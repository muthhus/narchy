package nars.nal.nal7;

import nars.NAR;
import nars.concept.Concept;
import nars.nar.Default;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

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

    @Test public void testInductionStability() {
        //two entirely disjoint events, and all inductable beliefs from them, should produce a finite system that doesn't explode
        Default d = new Default(256,1,2,3);
        d.input("a:b. :|:");
        d.frame(5);
        d.input("c:d. :|:");

        d.frame(24);

        //everything should be inducted by now:
        int numConcepts = d.core.active.size();
        int numBeliefs = getBeliefCount(d);

        System.out.println(numConcepts + " " + numBeliefs);

        d.frame(300);

        //# unique concepts unchanged:
        assertEquals(numConcepts, d.core.active.size());
        assertEquals(numBeliefs, getBeliefCount(d));

    }

    private static int getBeliefCount(NAR n) {
        AtomicInteger a = new AtomicInteger(0);
        n.forEachConceptTask(true,false,false,false,false,1000,t->{
           a.addAndGet(1);
        });
        return a.intValue();
    }
}
