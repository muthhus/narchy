package nars.net;

import nars.NAR;
import nars.nar.Default;
import nars.time.Tense;
import nars.util.Util;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static nars.$.$;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 7/8/16.
 */
@Ignore
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

            Util.pause(500);

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

    /** cooperative solving */
    @Test public void testInterNAR2() {

        testAB((ai, bi) -> {

            NAR a = ai.nar;
            NAR b = bi.nar;
            b.log();


            b.believe("(a --> b)");
            b.believe("(c --> d)");

            a.believe(0.9f, $("(b --> c)"), Tense.Eternal,1f,0.9f);

            Util.pause(500);

            AtomicBoolean recv = new AtomicBoolean();
            b.onTask(tt -> {
                //System.out.println(b + ": " + tt);
                if (tt.isBelief() && (!tt.isInput()) && tt.toString().contains("(a-->d)"))
                    recv.set(true);
            });
            b.ask("(a --> d)");

            a.run(16);
            b.run(16);

            Util.pause(200);

            assertTrue(recv.get());

        });


    }

}