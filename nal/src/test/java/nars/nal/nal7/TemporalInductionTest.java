package nars.nal.nal7;

import nars.NAR;
import nars.Narsese;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.nar.NARBuilder;
import nars.nar.Terminal;
import nars.table.BeliefTable;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 6/8/15.
 */
public class TemporalInductionTest {

    @Test
    public void testTemporalInduction() throws Narsese.NarseseException {

        String task = "<a --> b>. :|:";
        String task2 = "<c --> d>. :|:";

        NAR n = new NARBuilder().get();

        //TextOutput.out(n);

        n.input(task);
        n.run(10);
        n.input(task2);

        n.run(10);

    }

    @Test public void testTemporalRevision() throws Narsese.NarseseException {

        NAR n = new Terminal();
        n.time.dur(6);

        //TextOutput.out(n);

        n.input("a:b. %1.0|0.9%");
        n.run(5);
        n.input("a:b. %0.0|0.9%");
        n.run(1);

        //n.forEachActiveConcept(Concept::print);

        TaskConcept c = (TaskConcept) n.concept("a:b");
        //assertEquals("(b-->a). 5+0 %.50;.95%", c.getBeliefs().top(n.time()).toStringWithoutBudget());

        BeliefTable b = c.beliefs();
        assertEquals(2, b.size());

        //when originality is considered:
        //assertEquals("(b-->a). 5+0 %0.0;.90%", c.beliefs().top(n.time()).toStringWithoutBudget());

        //least relevant
        int dur = n.dur();
        assertEquals("(b-->a). 0 %1.0;.90%", n.belief(c, 0).toStringWithoutBudget());

        //most current relevant overall:
        assertEquals("(b-->a). 5 %0.0;.90%", n.belief(c, n.time()).toStringWithoutBudget());
    }

    @Test public void testTemporalRevisionOfTemporalRelation() throws Narsese.NarseseException {

        NAR n = new NARBuilder().get();

        //TextOutput.out(n);

        n.input("(a ==>+0 b). %1.0;0.7%");
        n.input("(a ==>+5 b). %1.0;0.6%");
        n.run(1);

        //n.forEachActiveConcept(Concept::print);

        //Concept c = n.concept("a:b");
        //assertEquals("(b-->a). 5+0 %.50;.95%", c.getBeliefs().top().toStringWithoutBudget());
    }
    @Test public void testQuestionProjection() throws Narsese.NarseseException {

        NAR n = new NARBuilder().get();

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
        NAR d = new NARBuilder().get();
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
        n.forEachConceptTask(true,false,false,false,false,1000, t->{
           a.addAndGet(1);
        });
        return a.intValue();
    }
}
