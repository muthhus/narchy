package nars.inter;

import nars.NAR;
import nars.nar.Default;
import nars.util.IO;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Created by me on 7/8/16.
 */
public class InterNARTest {

    @Test
    public void testInterNAR1() throws IOException, InterruptedException {

        NAR a = new Default().setSelf("a");
        InterNAR ai = new InterNAR(a);

        NAR b = new Default().setSelf("b");
        InterNAR bi = new InterNAR(b);

        bi.connect(ai);

        b.believe("(X --> y)");

        String question = "(?x --> y)?";
        ai.query(IO.asBytes(a.task(question)));
        //TODO a.ask(question);

        Thread.sleep(300);

        //a.log();
        AtomicBoolean recv = new AtomicBoolean();
        a.onTask(tt -> {
            //System.out.println(b + ": " + tt);
            if (tt.toString().contains("(X-->y)"))
                recv.set(true);
        });

        a.run(4);
        b.run(4);

        Thread.sleep(300);


        ai.stop();
        bi.stop();

        ai = bi = null;
        a = b = null;

    }
}