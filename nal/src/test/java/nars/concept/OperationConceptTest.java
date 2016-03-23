package nars.concept;

import com.google.common.base.Joiner;
import com.gs.collections.api.tuple.primitive.FloatFloatPair;
import com.gs.collections.impl.tuple.primitive.PrimitiveTuples;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.nal.nal8.operator.SyncOperator;
import nars.nal.nal8.operator.TermFunction;
import nars.nar.Default;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.List;

import static nars.util.Texts.n2;
import static org.junit.Assert.*;

/**
 * Created by me on 3/22/16.
 */
public class OperationConceptTest {

    @Test
    public void testMotivationBalanceEternal() {


        List<String> history = Global.newArrayList();

        NAR n = new Default();
        n.onExec(new SyncOperator("f") {

            @Override public void execute(Task execution) {
                float b = nar.concept(execution.term()).beliefMotivation(nar.time());
                float g = nar.concept(execution.term()).goalMotivation(nar.time());
                history.add(nar.time() + ":(" + n2(b) + "," + n2(g) + ")");
            }

        });

        Term op = $.$("f(x)");

        n.goal(op, 1f, 0.9f).step();
        assertMotive(n, op, 0, 0.9f);

        n.believe(op, 0f, 0.5f).step();
        assertMotive(n, op, -0.5f, 0.9f);

        n.believe(op, 1f, 0.6f).step().step(); //cause revision
        assertMotive(n, op, 0.14f, 0.9f);

        //n.concept(op).print();

        System.out.println(Joiner.on('\n').join(history));

        //number of execution state changes invoked
        assertEquals(4, history.size());
    }

    public static void assertMotive(NAR n, Termed operation, float b, float g) {
        assertEquals(b, n.concept(operation).beliefMotivation(n.time()), 0.01f);
        assertEquals(g, n.concept(operation).goalMotivation(n.time()), 0.01f);
    }
}