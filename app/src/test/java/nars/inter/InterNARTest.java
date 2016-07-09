package nars.inter;

import nars.NAR;
import nars.nar.Default;
import nars.util.IO;
import nars.util.Util;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static org.junit.Assert.*;

/**
 * Created by me on 7/8/16.
 */
public class InterNARTest {

    static void testAB(BiConsumer<InterNAR, InterNAR> ab) {

        try {
            NAR a = new Default().setSelf("a");
            InterNAR ai = new InterNAR(a);

            NAR b = new Default().setSelf("b");
            InterNAR bi = new InterNAR(b);

            bi.connect(ai);

            ab.accept(ai, bi);

            ai.stop();
            bi.stop();
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(e.toString(), false);
        }
    }

    @Test
    public void testInterNAR1() {

        testAB((ai, bi) -> {

            NAR a = ai.nar;
            NAR b = bi.nar;

            b.believe("(X --> y)");

            String question = "(?x --> y)?";
            a.ask(question);
            //ai.query(IO.asBytes(a.task(question)));
            //TODO a.ask(question);

            Util.pause(100);

            //a.log();
            AtomicBoolean recv = new AtomicBoolean();
            a.onTask(tt -> {
                //System.out.println(b + ": " + tt);
                if (tt.toString().contains("(X-->y)"))
                    recv.set(true);
            });

            a.run(4);
            b.run(4);

            Util.pause(200);

            assertTrue(recv.get());

        });


    }


}