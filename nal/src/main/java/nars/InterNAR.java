package nars;

import jcog.Util;
import jcog.net.UDPeer;
import nars.bag.leak.LeakOut;
import nars.control.CauseChannel;
import nars.op.stm.TaskService;
import nars.task.ActiveQuestionTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.function.BiConsumer;

import static jcog.net.UDPeer.Command.TELL;

/**
 * InterNAR P2P Network Interface for a NAR
 */
public class InterNAR extends TaskService implements BiConsumer<ActiveQuestionTask, Task> {

    //public static final Logger logger = LoggerFactory.getLogger(InterNAR.class);

    public final LeakOut buffer;
    final CauseChannel<Task> recv;
    public MyUDPeer peer;


    @Override
    public void accept(@NotNull Task t) {
        buffer.accept(t);
    }

    @Override
    protected void startUp() throws Exception {
        peer = new MyUDPeer(0, true);
    }

    @Override
    protected void shutDown() throws Exception {
        peer.stop();
        peer = null;
    }


    InterNAR pri(float priFactor) {
        recv.amplitude = priFactor;
        return InterNAR.this;
    }

    /**
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
     * @param nar
     * @param outRate  output rate in tasks per cycle, some value > 0, ammortize over multiple cycles with a fraction < 1
     * @param port
     * @param discover
     * @throws SocketException
     * @throws UnknownHostException
     */
    public InterNAR(NAR nar, float outRate, int port, boolean discover) throws IOException {
        super(nar);

        peer = new MyUDPeer(port, discover);

        recv = nar.newInputChannel(this);

        buffer = new LeakOut(nar, 256, outRate) {
            @Override
            protected float send(Task x) {

                if (peer.connected()) {
                    try {
                        x = nar.post(x);
                        //if (x!=null) {
                        @Nullable byte[] msg = IO.taskToBytes(x);
                        if (msg != null) {
                            if (peer.tellSome(msg, ttl(x), true) > 0) {
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
            public void accept(@NotNull Task t) {
                if (t.isCommand() || !peer.connected())
                    return;

                super.accept(t);
            }
        };
    }

    public void ping(InetSocketAddress x) {
        peer.ping(x);
    }

    private static byte ttl(Task x) {
        return (byte) (1 + Util.lerp(x.priElseZero() /* * (1f + x.qua())*/, 2, 5));
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
    public void accept(ActiveQuestionTask question, Task answer) {
        UDPeer.Msg q = question.meta(UDPeer.Msg.class);
        if (q == null)
            return;

        answer = nar.post(answer);
        @Nullable byte[] a = IO.taskToBytes(answer);
        if (a != null) {
            UDPeer.Msg aa = new UDPeer.Msg(TELL.id, ttl(answer), peer.me, null, a);
            if (!peer.seen(aa, 1f))
                peer.send(aa, q.origin());
        }


    }

    public void runFPS(float fps) {
        peer.runFPS(fps);
    }

    public InetSocketAddress addr() {
        return peer.addr;
    }

    private class MyUDPeer extends UDPeer {

        public MyUDPeer(int port, boolean discovery) throws IOException {
            super(port, discovery);
        }

        @Override
        public void stop() {
            super.stop();
        }

        @Override
        protected void onTell(UDProfile connected, Msg m) {

            Task x;


            x = IO.taskFromBytes(m.data());
            if (x != null) {
                if (x.isQuestOrQuestion()) {
                    //reconstruct a question task with an onAnswered handler to reply with answers to the sender
                    x = new ActiveQuestionTask(x, 8, nar, InterNAR.this);
                    x.meta(Msg.class, m);
                }
                x.budget(nar);

                //System.out.println(me + " RECV " + x + " " + Arrays.toString(x.stamp()) + " from " + m.origin());
                logger.debug("recv {} from {}", x, m.origin());
                recv.input(x);
            }
        }
    }
}
