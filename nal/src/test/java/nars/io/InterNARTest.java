package nars.io;

import jcog.Util;
import nars.InterNAR;
import nars.NAR;
import nars.Narsese;
import nars.Param;
import nars.conceptualize.DefaultConceptBuilder;
import nars.index.term.map.CaffeineIndex;
import nars.nar.Default;
import nars.time.RealTime;
import nars.time.Tense;
import nars.util.exe.BufferedSynchronousExecutor;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static nars.$.$;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 7/8/16.
 */
public class InterNARTest {

    final static AtomicInteger nextPort = new AtomicInteger(10000);

    static void testAB(BiConsumer<NAR, NAR> beforeConnect, BiConsumer<InterNAR, InterNAR> afterConnect) {

        final int CONNECTION_TIME = 50;

        Param.ANSWER_REPORTING = false;

        try {
            NAR a = newNAR();
            a.setSelf("a");

            NAR b = newNAR();
            b.setSelf("b");

            beforeConnect.accept(a, b);

            a.run(1); b.run(1);

            InterNAR ai = new InterNAR(a, 1, nextPort.incrementAndGet());

            InterNAR bi = new InterNAR(b, 1, nextPort.incrementAndGet());

            Util.pause(CONNECTION_TIME);

            bi.ping(ai.port());

            Util.pause(CONNECTION_TIME);

            afterConnect.accept(ai, bi);

            Util.pause(CONNECTION_TIME);

            int postCycles = 16;
            for (int i = 0; i < postCycles; i++) {
                a.run(1);
                b.run(1);
            }

            Util.pause(CONNECTION_TIME);

            ai.stop();
            a.stop();
            bi.stop();
            b.stop();

        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(e.toString(), false);
        }
    }

    private static Default newNAR() {
        return new Default(1024,
                new CaffeineIndex(new DefaultConceptBuilder(), 1024, false, null),
                new RealTime.DSHalf(true), new BufferedSynchronousExecutor());
    }

    @Test
    public void testInterNAR1() {
        AtomicBoolean recv = new AtomicBoolean();

        testAB((a, b) -> {

            try {
                b.believe("(X --> y)");
            } catch (Narsese.NarseseException e) {
                assertTrue(false);
            }

            a.onTask(tt -> {
                //System.out.println(b + ": " + tt);
                if (tt.toString().contains("(X-->y)"))
                    recv.set(true);
            });

            //a.log();

        }, (ai, bi) -> {


            String question = "(?x --> y)?";
            try {
                ai.nar.ask(question);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

        });

        assertTrue(recv.get());

    }

    /** cooperative solving */
    @Test public void testInterNAR2() {

        AtomicBoolean recv = new AtomicBoolean();

        testAB((a, b) -> {


            try {
                b.believe("(a --> b)");
                b.believe("(c --> d)");
            } catch (Narsese.NarseseException e) {
                assertTrue(false);
            }

            b.onTask(tt -> {
                //System.out.println(b + ": " + tt);
                if (tt.isBelief() && tt.toString().contains("(a-->d)"))
                    recv.set(true);
            });

        }, (ai, bi) -> {


            try {
                bi.nar.ask("(a --> d)");
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

            try {
                ai.nar.believe(0.5f, $("(b --> c)"), Tense.Eternal,1f,0.9f);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }




        });

        assertTrue(recv.get());

    }

}