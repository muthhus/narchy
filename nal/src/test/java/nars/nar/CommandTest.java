package nars.nar;

import nars.NAR;
import nars.Symbols;
import nars.Task;
import nars.concept.Concept;
import nars.concept.OperationConcept;
import nars.nal.nal8.operator.NullOperator;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;


public class CommandTest {

    @Test
    public void testEcho() {
        NAR n = new Default();
        AtomicBoolean invoked = new AtomicBoolean();
        n.onExec(new NullOperator("c") {

            @Override
            public void execute(@NotNull OperationConcept t) {

                invoked.set(true);
//                Term[] a = Operator.argArray(t.term());
//                assertEquals(1, a.length);
//                assertEquals("x", a[0].toString());

            }
        });
        Task t = n.task("c(x);");
        assertNotNull(t);
        assertEquals(Symbols.COMMAND, t.punc());
        assertTrue(t.isCommand());
        assertEquals("c(x); :0:", t.toString());

        n.input(t);

        n.run(1);

        assertTrue(invoked.get());

        //no concepts created because this command bypassed inference
        n.index.forEach(c -> assertFalse(c instanceof Concept));

    }
}
