package nars.concept;

import com.google.common.base.Joiner;
import nars.Global;
import nars.NAR;
import nars.nar.Default;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;

import static nars.util.Texts.n2;
import static org.junit.Assert.assertEquals;

/**
 * Created by me on 3/22/16.
 */
public class OperationConceptTest {

    @Test
    public void testMotivationBalanceEternal() {

        List<String> history = Global.newArrayList();

        NAR n = new Default();

        Termed op = new OperationConcept("f(x)", n) {
            @Override public void accept(@NotNull NAR nar) {
                history.add(nar.time() + ":(" + n2(belief(nar.time()).expectation()) + "," + n2(desire(nar.time()).expectation()) + ")");
            }
        };


        n.goal(op, 1f, 0.9f);
        n.step().step();
        assertMotive(n, op, 0.5f, 0.95f);

        n.believe(op, 0f, 0.5f).step();
        assertMotive(n, op, 0.25f, 0.95f);

        n.believe(op, 1f, 0.6f).step().step(); //cause revision
        assertMotive(n, op, 0.571f, 0.95f);

        //n.concept(op).print();

        System.out.println(Joiner.on('\n').join(history));

        //number of execution state changes invoked
        assertEquals(4, history.size());
    }

    public static void assertMotive(@NotNull NAR n, @NotNull Termed operation, float b, float g) {
        assertEquals(b, n.concept(operation).belief(n.time()).expectation(), 0.01f);
        assertEquals(g, n.concept(operation).desire(n.time()).expectation(), 0.01f);
    }
}