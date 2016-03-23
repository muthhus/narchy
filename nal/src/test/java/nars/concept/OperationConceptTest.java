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

        Termed op = new OperationConcept("f(x)", n) {
            @Override protected void update(float belief, float desire, long now) {
                super.update(belief, desire, now);
                history.add(now + ":(" + n2(belief) + "," + n2(desire) + ")");
            }
        };


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
        assertEquals(b, n.concept(operation).beliefMotivation(n.time(), n.duration()), 0.01f);
        assertEquals(g, n.concept(operation).goalMotivation(n.time(), n.duration()), 0.01f);
    }
}