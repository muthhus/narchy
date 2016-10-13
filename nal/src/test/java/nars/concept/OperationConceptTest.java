package nars.concept;

import nars.NAR;
import nars.nar.Default;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 3/22/16.
 */
public class OperationConceptTest {

    @Test
    public void testMotivationBalanceEternal() {

//        List<String> history = $.newArrayList();

        NAR n = new Default();

        Termed op = new OperationConcept("f(x)", n) {
//            @Override public void accept(@NotNull NAR nar) {
//                history.add(nar.time() + ":(" + n2(belief(nar.time()).expectation()) + "," + n2(desire(nar.time()).expectation()) + ")");
//            }
        };


        n.goal(op, 1f, 0.9f);
        n.next().next();
        assertMotive(n, op, Float.NaN, 0.95f);

        n.believe(op, 0f, 0.5f).next();
        assertMotive(n, op, 0.25f, 0.95f);

        n.believe(op, 1f, 0.6f).next().next(); //cause revision
        assertMotive(n, op, 0.571f, 0.95f);

        //n.concept(op).print();

//        System.out.println(Joiner.on('\n').join(history));
//
//        //number of execution state changes invoked
//        assertEquals(4, history.size());
    }

    public static void assertMotive(@NotNull NAR n, @NotNull Termed operation, float b, float g) {
        Concept c = n.concept(operation);
        assertEquals(g, c.goal(n.time()).expectation(), 0.01f);
        if (b != b /* NaN */)
            assertTrue(c.beliefs().isEmpty());
        else
            assertEquals(b, c.belief(n.time()).expectation(), 0.01f);

        //assertEquals(b, n.concept(operation).belief(n.time()).expectation(), 0.01f);

    }


}