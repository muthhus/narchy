package nars.nal.nal7;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.BaseConcept;
import nars.table.BeliefTable;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Created by me on 6/8/15.
 */
public class TemporalInductionTest {

    @Test
    public void testTemporalInduction() throws Narsese.NarseseException {

        String task = "<a --> b>. :|:";
        String task2 = "<c --> d>. :|:";

        NAR n = new NARS().get();

        //TextOutput.out(n);

        n.input(task);
        n.run(10);
        n.input(task2);

        n.run(10);

    }

    @Test public void testTemporalRevision() throws Narsese.NarseseException {

        NAR n = new NARS().get();
        n.time.dur(1);

        n.log();
        //TextOutput.out(n);

        n.input("a:b. %1.0|0.9%");
        n.run(5);
        n.input("a:b. %0.0|0.9%");
        n.run(5);
        n.input("a:b. %0.5|0.9%");
        n.run(1);

        //n.forEachConcept(Concept::print);

        BaseConcept c = (BaseConcept) n.conceptualize("a:b");
        assertNotNull(c);
        //assertEquals("(b-->a). 5+0 %.50;.95%", c.getBeliefs().top(n.time()).toStringWithoutBudget());

        BeliefTable b = c.beliefs();
        b.print();
        assertTrue(3 <= b.size());

        //when originality is considered:
        //assertEquals("(b-->a). 5+0 %0.0;.90%", c.beliefs().top(n.time()).toStringWithoutBudget());

        //most current relevant overall:
        assertEquals(
                "(b-->a). 5 %0.0;.85%"
                , n.belief(c.term(), 5).toStringWithoutBudget());


        //least relevant
        assertEquals(
                "(b-->a). 0 %1.0;.90%"
                , n.belief(c.term(), 0).toStringWithoutBudget());

    }

    @Test public void testTemporalRevisionOfTemporalRelation() throws Narsese.NarseseException {

        NAR n = new NARS().get();

        //TextOutput.out(n);

        n.input("(a ==>+0 b). %1.0;0.7%");
        n.input("(a ==>+5 b). %1.0;0.6%");
        n.run(1);

        //n.forEachActiveConcept(Concept::print);

        //Concept c = n.concept("a:b");
        //assertEquals("(b-->a). 5+0 %.50;.95%", c.getBeliefs().top().toStringWithoutBudget());
    }
    @Test public void testQuestionProjection() throws Narsese.NarseseException {

        NAR n = new NARS().get();

        n.log();

        n.input("a:b. :|:");
        //n.frame();
        n.input("a:b? :/:");
        n.run(5);
        n.input("a:b? :/:");
        n.run(30);
        n.input("a:b? :/:");
        n.run(250);
        n.input("a:b? :/:");
        n.run(1);

        //n.forEachConcept(Concept::print);

        //Concept c = n.concept("a:b");
        //assertEquals("(b-->a). 5+0 %.50;.95%", c.getBeliefs().top().toStringWithoutBudget());
    }

    @Test public void testInductionStability() throws Narsese.NarseseException {
        //two entirely disjoint events, and all inductable beliefs from them, should produce a finite system that doesn't explode
        NAR d = new NARS().get();
        d.input("a:b. :|:");
        d.run(5);
        d.input("c:d. :|:");

        d.run(200);

        //everything should be inducted by now:
        int before = d.terms.size();
        int numBeliefs = getBeliefCount(d);

        //System.out.println(numConcepts + " " + numBeliefs);

        d.run(60);

        //# unique concepts unchanged:
        int after = d.terms.size();
        assertEquals(before, after);
        assertEquals(numBeliefs, getBeliefCount(d));

    }

    private static int getBeliefCount(@NotNull NAR n) {
        AtomicInteger a = new AtomicInteger(0);
        n.tasks(true,false,false,false).forEach(t->{
           a.addAndGet(1);
        });
        return a.intValue();
    }
}
