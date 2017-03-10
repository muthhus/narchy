package nars.net;

import jcog.Util;
import jcog.net.UDPeer;
import jcog.random.XORShiftRandom;
import jcog.random.XorShift128PlusRandom;
import nars.*;
import nars.conceptualize.DefaultConceptBuilder;
import nars.index.term.map.CaffeineIndex;
import nars.nar.Default;
import nars.time.RealTime;
import nars.time.Tense;
import org.junit.Ignore;
import org.junit.Test;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static nars.$.$;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 7/8/16.
 */
@Ignore
public class InterNARTest {

    public static class InterNAR extends UDPeer {

        private final NAR nar;

        public InterNAR(NAR nar, int port) throws SocketException, UnknownHostException {
            super(port);
            this.nar = nar;
            nar.onTask(x -> {
                if (x.isCommand())
                    return;

                say(IO.taskToBytes(x), 3, true);
            });
        }

//        @Override
//        public int send(Msg o, float pri, boolean onlyIfNotSeen) {
//
//            int sent = super.send(o, pri, onlyIfNotSeen);
//            if (sent > 0)
//                System.out.println(me + " SEND " + o + " to " + sent);
//
//            return sent;
//        }

        @Override
        protected void receive(UDPeer.Msg m) {

            Task x = IO.taskFromBytes(m.data(), nar.concepts);
            //System.out.println(me + " RECV " + x + " " + Arrays.toString(x.stamp()) + " from " + m.origin());
            nar.input(x);
        }

    }

    final static AtomicInteger nextPort = new AtomicInteger(10000);

    static void testAB(BiConsumer<NAR, NAR> beforeConnect, BiConsumer<InterNAR, InterNAR> afterConnect) {

        final int CONNECTION_TIME = 100;
        Param.ANSWER_REPORTING = false;

        try {
            NAR a = newNAR();
            a.setSelf("a");

            NAR b = newNAR();
            b.setSelf("b");

            beforeConnect.accept(a, b);

            a.run(1); b.run(1);

            InterNAR ai = new InterNAR(a, nextPort.incrementAndGet());

            InterNAR bi = new InterNAR(b, nextPort.incrementAndGet());

            bi.ping(ai.me());

            Util.pause(CONNECTION_TIME);

            afterConnect.accept(ai, bi);

            a.run(1); b.run(1);

            ai.stop();
            bi.stop();
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(e.toString(), false);
        }
    }

    private static Default newNAR() {
        return new Default(1024, 1, 1, 3, new XorShift128PlusRandom(System.nanoTime()),
                new CaffeineIndex(new DefaultConceptBuilder(), 1024, false, null),
                new RealTime.DSHalf());
    }

    @Test
    public void testInterNAR1() {

        testAB((a, b) -> {

            b.believe("(X --> y)");

            a.log();

        }, (ai, bi) -> {

            NAR a = ai.nar;
            NAR b = bi.nar;


            String question = "(?x --> y)?";
            try {
                a.ask(question);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

            Util.pause(500);

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

        testAB((ai, bi) -> {}, (ai, bi) -> {

            NAR a = ai.nar;
            NAR b = bi.nar;

            b.believe("(a --> b)");
            b.believe("(c --> d)");

            try {
                a.believe(0.9f, $("(b --> c)"), Tense.Eternal,1f,0.9f);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

            Util.pause(500);

            AtomicBoolean recv = new AtomicBoolean();
            b.onTask(tt -> {
                //System.out.println(b + ": " + tt);
                if (tt.isBelief() && (!tt.isInput()) && tt.toString().contains("(a-->d)"))
                    recv.set(true);
            });
            try {
                b.ask("(a --> d)");
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

            a.run(16);
            b.run(16);

            Util.pause(200);

            assertTrue(recv.get());

        });


    }

}