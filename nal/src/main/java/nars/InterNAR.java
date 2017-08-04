package nars;

import jcog.Util;
import jcog.net.UDPeer;
import jcog.pri.PriReference;
import nars.bag.leak.LeakOut;
import nars.control.CauseChannel;
import nars.task.ActiveQuestionTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static jcog.net.UDPeer.Command.TELL;

/**
 * InterNAR P2P Network Interface for a NAR
 */
public class InterNAR extends UDPeer implements BiConsumer<ActiveQuestionTask, Task> {

    //public static final Logger logger = LoggerFactory.getLogger(InterNAR.class);

    /** tasks per second output */
    private static final float DEFAULT_RATE = 8;

    public final NAR nar;
    public final LeakOut send;
    public final CauseChannel<Task> recv;


    public InterNAR(NAR nar) throws IOException {
        this(nar, DEFAULT_RATE, 0);
    }

    InterNAR pri(float priFactor) {
        recv.amplitude = priFactor;
        return this;
    }

    /**
     *
     * @param nar
     * @param outRate output rate in tasks per cycle, some value > 0, ammortize over multiple cycles with a fraction < 1
     * @param port
     * @throws SocketException
     * @throws UnknownHostException
     */
    public InterNAR(NAR nar, float outRate, int port) throws IOException {
        this(nar, outRate, port, true);
    }

    /**
     *
     * @param nar
     * @param outRate output rate in tasks per cycle, some value > 0, ammortize over multiple cycles with a fraction < 1
     * @param port
     * @param discover
     * @throws SocketException
     * @throws UnknownHostException
     */
    public InterNAR(NAR nar, float outRate, int port, boolean discover) throws IOException {
        super(port, discover);
        this.nar = nar;

        this.recv = nar.newInputChannel(this);

        this.send = new LeakOut(nar, 256, outRate) {
            @Override protected float send(Task x) {

                if (connected()) {
                    try {
                        x = nar.post(x);
                        //if (x!=null) {
                            @Nullable byte[] msg = IO.taskToBytes(x);
                            if (msg != null) {
                                if (tellSome(msg, ttl(x), true) > 0) {
                                    return 1;
                                }
                            }
                        //}
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
                return 0;

            }

            @Override
            protected void in(@NotNull Task t, Consumer<PriReference<Task>> each) {
                if (t.isCommand() || !connected())
                    return;

                super.in(t, each);
            }
        };
    }

    private static byte ttl(Task x) {
        return (byte)(1 + Util.lerp(x.priElseZero() /* * (1f + x.qua())*/, 2, 5));
    }

    @Override
    public void stop() {
        super.stop();
        send.stop();
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
    protected void onTell(UDPeer.UDProfile connected, Msg m) {

        Task x;

        try {
            x = IO.taskFromBytes(m.data());
        } catch (RuntimeException e) {
            logger.warn("bad Task: {} len={}", m, m.dataLength() );
            return;
        }

        if (x!=null) {
            if (x.isQuestOrQuestion()) {
                //reconstruct a question task with an onAnswered handler to reply with answers to the sender
                x = new ActiveQuestionTask(x, 8, nar, this);
                x.meta(Msg.class, m);
            }
            x.budget(nar);

            //System.out.println(me + " RECV " + x + " " + Arrays.toString(x.stamp()) + " from " + m.origin());
            logger.debug("recv {} from {}", x, m.origin());
            recv.input(x);
        }
    }

    @Override
    public void accept(ActiveQuestionTask question, Task answer) {
        Msg q = question.meta(Msg.class);
        if (q==null)
            return;

        try {
            answer = nar.post(answer);
            if (answer!=null) {
                @Nullable byte[] a = IO.taskToBytes(answer);
                if (a != null) {
                    Msg aa = new Msg(TELL.id, ttl(answer), me, null, a);
                    if (!seen(aa, 1f))
                        send(aa, q.origin());
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

    }
}
