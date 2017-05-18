package nars.nar;

import nars.NAR;
import nars.Narsese;
import nars.Task;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static nars.Op.COMMAND;
import static org.junit.Assert.*;


@Ignore
public class CommandTest {

    @Test
    public void testEcho() throws Narsese.NarseseException {
        NAR n = new Default();
        AtomicBoolean invoked = new AtomicBoolean();
        n.on("c", (args) -> {
            assertEquals("(x)", args.toString());
            invoked.set(true);
            return null;
        });
        Task t = n.task("c(x);");
        assertNotNull(t);
        assertEquals(COMMAND, t.punc());
        assertTrue(t.isCommand());
        assertEquals("c(x);", t.toString());

        n.input(t);
        n.run(1);

        assertTrue(invoked.get());


    }
}
