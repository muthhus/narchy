package nars;

import jcog.Util;
import jcog.net.UDPeer;
import nars.bag.leak.LeakOut;
import nars.budget.BLink;
import nars.task.LambdaQuestionTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * InterNAR P2P Network Interface for a NAR
 */
public class InterNAR extends UDPeer implements BiConsumer<LambdaQuestionTask, Task> {

    public static final Logger logger = LoggerFactory.getLogger(InterNAR.class);

    public final NAR nar;
    public final LeakOut out;

    /**
     *
     * @param nar
     * @param outRate output rate in tasks per cycle, some value > 0, ammortize over multiple cycles with a fraction < 1
     * @param port
     * @throws SocketException
     * @throws UnknownHostException
     */
    public InterNAR(NAR nar, float outRate, int port) throws SocketException, UnknownHostException {
        super(port);
        this.nar = nar;
        this.out = new LeakOut(nar, 16, outRate) {
            @Override protected float send(Task x) {

                if (!them.isEmpty()) {
                    try {
                        x = nar.post(x);
                        if (x!=null) {
                            @Nullable byte[] msg = IO.taskToBytes(x);
                            if (msg != null) {
                                if (say(msg, ttl(x), true) > 0) {
                                    return 1;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return 0;

            }

            @Override
            protected void in(@NotNull Task t, Consumer<BLink<Task>> each) {
                if (t.isCommand())
                    return;

                super.in(t, each);
            }
        };
        logger.info("start");
    }

    private static byte ttl(Task x) {
        return (byte)Util.lerp((1f + x.pri()) * (1f + x.qua()), 5, 2);
    }

    @Override
    public synchronized void stop() {
        out.stop();
        super.stop();
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
    protected void receive(Msg m) {

        Task x = IO.taskFromBytes(m.data(), nar.concepts);
        if (x!=null) {
            if (x.isQuestOrQuestion()) {
                //reconstruct a question task with an onAnswered handler to reply with answers to the sender
                x = new LambdaQuestionTask(x, 8, nar, this);
                x.meta(Msg.class, m);
            }

            //System.out.println(me + " RECV " + x + " " + Arrays.toString(x.stamp()) + " from " + m.origin());
            nar.input(x);
        }
    }

    @Override
    public void accept(LambdaQuestionTask question, Task answer) {
        Msg q = question.meta(Msg.class);
        if (q==null)
            return;

        try {
            answer = nar.post(answer);
            if (answer!=null) {
                @Nullable byte[] a = IO.taskToBytes(answer);
                if (a != null) {
                    Msg aa = new Msg(SAY, ttl(answer), id, null, a);
                    if (!seen(aa, 1f))
                        send(aa, q.origin());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
