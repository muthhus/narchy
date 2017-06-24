package nars.io;

import jcog.Util;
import nars.InterNAR;
import nars.NAR;
import nars.Narsese;
import nars.Param;
import nars.conceptualize.DefaultConceptBuilder;
import nars.derive.DefaultDeriver;
import nars.derive.Deriver;
import nars.index.term.map.MapTermIndex;
import nars.nar.NARBuilder;
import nars.time.RealTime;
import nars.time.Tense;
import nars.util.exe.TaskExecutor;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static nars.$.$;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 7/8/16.
 */
public class InterNARTest {

    static {
        Deriver x = DefaultDeriver.the;
    }

    void testAB(BiConsumer<NAR, NAR> beforeConnect, BiConsumer<InterNAR, InterNAR> afterConnect) {

        final int CONNECTION_TIME = 200;
        int preCycles = 15;
        int postCycles = 500;

        Param.ANSWER_REPORTING = false;

        try {
            NAR a = newNAR();
            a.setSelf("a");

            NAR b = newNAR();
            b.setSelf("b");

            beforeConnect.accept(a, b);

            for (int i = 0; i < preCycles; i++) {
                a.run(1); b.run(1);
            }

            InterNAR ai = new InterNAR(a, 10, 0, false);
            InterNAR bi = new InterNAR(b, 10, 0, false);

            ai.runFPS(20f);
            bi.runFPS(20f);

            Util.sleep(CONNECTION_TIME);

            bi.ping(ai.addr);

            Util.sleep(CONNECTION_TIME);

            afterConnect.accept(ai, bi);

            Util.sleep(CONNECTION_TIME);

            for (int i = 0; i < postCycles; i++) {
                a.run(1); b.run(1);
            }

            Util.sleep(CONNECTION_TIME);

            ai.stop();
            bi.stop();
            a.stop();
            b.stop();

        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(e.toString(), false);
        }
    }

    private static NAR newNAR() {
        return new NARBuilder().index(new MapTermIndex(new DefaultConceptBuilder(), new ConcurrentHashMap(1024))).time(new RealTime.DSHalf(true)).exe(new TaskExecutor(256)).get();
    }

    @Test
    public void testInterNAR1() {
        AtomicBoolean recv = new AtomicBoolean();

        testAB((a, b) -> {

             a.onTask(tt -> {
                //System.out.println(b + ": " + tt);
                if (tt.toString().contains("(X-->y)"))
                    recv.set(true);
            });

            try {
                b.believe("(X --> y)");
            } catch (Narsese.NarseseException e) {
                assertTrue(false);
            }



            //a.log();

        }, (ai, bi) -> {


            try {
                ai.nar.input("(?x --> y)?");
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
                bi.nar.question("(a --> d)");
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

            try {
                ai.nar.believe( $("(b --> c)"), Tense.Eternal,1f,0.9f);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }




        });

        assertTrue(recv.get());

    }

}