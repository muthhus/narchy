package nars;

import jcog.Util;
import jcog.net.UDPeer;
import nars.bag.leak.LeakOut;
import nars.budget.BLink;
import org.jetbrains.annotations.NotNull;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.function.Consumer;

/**
 * InterNAR P2P Network Interface for a NAR
 */
public class InterNAR extends UDPeer {

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
                if (!them.isEmpty() && say(IO.taskToBytes(x), Util.lerp(x.pri() * x.qua(), 3, 1), true) > 0)
                    return 1;
                else
                    return 0;
            }

            @Override
            protected void in(@NotNull Task t, Consumer<BLink<Task>> each) {
                if (t.isCommand())
                    return;
                super.in(t, each);
            }
        };
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
        //System.out.println(me + " RECV " + x + " " + Arrays.toString(x.stamp()) + " from " + m.origin());
        nar.input(x);
    }

}
