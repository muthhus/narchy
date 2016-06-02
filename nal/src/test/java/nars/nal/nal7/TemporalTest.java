package nars.nal.nal7;

import nars.NAR;
import nars.budget.UnitBudget;
import nars.concept.CompoundConcept;
import nars.index.TermIndex;
import nars.nar.Terminal;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class TemporalTest {

    @NotNull NAR n = new Terminal(128); //for cycle/frame clock, not realtime like Terminal


    @Test public void parsedCorrectOccurrenceTime() {
        Task t = n.inputTask("<a --> b>. :\\:");
        assertEquals(0, t.creation());
        assertEquals(-(1 /*n.duration()*/), t.occurrence());
    }

    @Test public void testCoNegatedSubtermConcept() {
        assertEquals("((--,(x))&&(x))", n.conceptualize(
                n.term("((x) &&+10 (--,(x)))"), UnitBudget.One).toString());
    }

    @Test public void testCoNegatedSubtermTask() {

        //allowed
        assertNotNull(n.task("((x) &&+1 (--,(x)))."));

        //not allowed
        assertInvalidTask("((x) && (--,(x))).");
        assertInvalidTask("((x) &&+0 (--,(x))).");
    }

    public void assertInvalidTask(@NotNull String ss) {
        try {
            n.input(ss);
            assertTrue(false);
        } catch (TermIndex.InvalidTaskTerm e) {
            assertTrue(true);
        }
    }

    @Test public void testAtemporalization() {
        assertEquals("((x)==>(y))", n.conceptualize(
                n.term("((x) ==>+10 (y))"), UnitBudget.One).toString());
    }

    @Test public void testAtemporalizationSharesNonTemporalSubterms() {

        Task a = n.inputTask("((x) ==>+10 (y)).");
        Task c = n.inputTask("((x) ==>+9 (y)).");
        Task b = n.inputTask("((x) <-> (y)).");

        assertTrue( n.concept(a.term()) == n.concept(c.term()));

        assertTrue( ((CompoundConcept)n.concept(a.term())).term(0) == ((CompoundConcept)n.concept(c.term())).term(0));
        assertTrue( ((CompoundConcept)n.concept(b.term())).term(0) == ((CompoundConcept)n.concept(c.term())).term(0));

    }

    @Test public void testParseOperationInFunctionalForm2() {
        assertEquals("(do(that) &&+0 ((a)&&(b)))", n.term("(do(that) &&+0 ((a)&&(b)))").toString());

        Termed<Term> nt = n.term("(((that)-->^do) &&+0 ((a)&&(b)))");
        assertEquals("(do(that) &&+0 ((a)&&(b)))", nt.toString());

        //assertNotNull(n.conceptualize(nt, UnitBudget.One));
        assertEquals("(do(that)&&((a)&&(b)))", n.conceptualize(nt, UnitBudget.One).toString());

        //assertEquals("(&&,do(that),(a),(b))", n.conceptualize(nt, UnitBudget.One).toString()); ????????

    }

    @Test public void testAnonymization2() {
        Termed<Term> nn = n.term("(do(that) &&+1 ((a) ==>+2 (b)))");
        assertEquals("(do(that) &&+1 ((a) ==>+2 (b)))", nn.toString());


        assertEquals("(do(that)&&((a)==>(b)))", n.conceptualize(nn, UnitBudget.One).toString());

        //assertEquals("(&&,do(that),(a),(b))", n.conceptualize(nt, UnitBudget.One).toString()); ??

    }
//    @Test
//    public void testAfter() {
//
//        assertTrue("after", Tense.after(1, 4, 1));
//
//        assertFalse("concurrent (equivalent)", Tense.after(4, 4, 1));
//        assertFalse("before", Tense.after(6, 4, 1));
//        assertFalse("concurrent (by duration range)", Tense.after(3, 4, 3));
//
//    }
}
