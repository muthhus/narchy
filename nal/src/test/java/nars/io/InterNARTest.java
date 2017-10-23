package nars.io;

import jcog.Util;
import nars.*;
import nars.time.Tense;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by me on 7/8/16.
 */
public class InterNARTest {

    void testAB(BiConsumer<NAR, NAR> beforeConnect, BiConsumer<NAR, NAR> afterConnect) {

        final int CONNECTION_TIME = 200;
        int preCycles = 15;
        int postCycles = 10;

        Param.ANSWER_REPORTING = false;

        try {
            NAR a = NARS.threadSafe();
            a.setSelf("a");

            NAR b = NARS.threadSafe();
            b.setSelf("b");

            beforeConnect.accept(a, b);

            for (int i = 0; i < preCycles; i++) {
                a.run(1);
                b.run(1);
            }

            InterNAR ai = new InterNAR(a, 10, 0, false) {
                @Override
                protected void start(NAR nar) {
                    super.start(nar);
                    nar.runLater(() -> {

                        runFPS(5f);

                        Util.sleep(CONNECTION_TIME);

                        afterConnect.accept(a, b);

                        a.run(postCycles);

                        a.stop();

                    });
                }
            };
            InterNAR bi = new InterNAR(b, 10, 0, false) {
                @Override
                protected void start(NAR nar) {
                    super.start(nar);
                    nar.runLater(() -> {
                        runFPS(5f);

                        Util.sleep(CONNECTION_TIME);

                        ping(ai.addr());

                        b.run(postCycles);

                        b.stop();
                    });
                }
            };

            Util.sleep(CONNECTION_TIME * 4);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }
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
                fail(e);
            }


            //a.log();

        }, (a, b) -> {


            try {
                a.input("(?x --> y)?");
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

        });

        assertTrue(recv.get());

    }

    /**
     * cooperative solving
     */
    @Test
    public void testInterNAR2() {

        AtomicBoolean recv = new AtomicBoolean();

        testAB((a, b) -> {


            try {
                b.believe("(a --> b)");
                b.believe("(c --> d)");
            } catch (Narsese.NarseseException e) {
                fail(e);
            }

            b.onTask(tt -> {
                //System.out.println(b + ": " + tt);
                if (tt.isBelief() && tt.toString().contains("(a-->d)"))
                    recv.set(true);
            });

        }, (a, b) -> {


            try {
                b.question("(a --> d)");
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

            try {
                a.believe($("(b --> c)"), Tense.Eternal, 1f, 0.9f);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }


        });

        assertTrue(recv.get());

    }

}