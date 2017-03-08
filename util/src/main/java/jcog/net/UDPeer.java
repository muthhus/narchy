package jcog.net;

import jcog.bag.Bag;
import jcog.bag.impl.HijackBag;
import jcog.data.random.XorShift128PlusRandom;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.eclipse.collections.impl.factory.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * UDP peer
 */
public class UDPeer extends UDP {

    public static final String PING = "P";
    public static final String PONG = "p";


    static class UDProfile {
        public final InetSocketAddress addr;

        final static int PING_WINDOW = 8;

        /** ping time, in ms */
        final SynchronizedDescriptiveStatistics pingTime = new SynchronizedDescriptiveStatistics(PING_WINDOW);

        long lastMessage = Long.MIN_VALUE;


        public UDProfile(InetSocketAddress addr, long initialPingTime) {
            this.addr = addr;
            onPing(initialPingTime);
        }

        public void onPing(long time) {
            pingTime.addValue(time);
        }

        /** average ping time in ms */
        public long latency() {
            return Math.round(pingTime.getMean());
        }
    }

    final static int maxPeers = 8;
    public final Bag<InetSocketAddress, UDProfile> them;

    public UDPeer(int port) throws SocketException {
        super(port);

        them = new HijackBag<InetSocketAddress, UDProfile>(4, new XorShift128PlusRandom(System.currentTimeMillis())) {

            @Override
            public void onAdded(UDProfile p) {
                System.out.println(UDPeer.this + " connected " + p.addr);
            }

            @Override
            public void onRemoved(@NotNull UDPeer.UDProfile p) {
                System.out.println(UDPeer.this + " disconnected " + p.addr);
            }

            @Override
            protected float merge(@Nullable UDPeer.UDProfile existing, @NotNull UDPeer.UDProfile incoming, float scale) {
                return 0;
            }

            @Override
            protected Consumer<UDProfile> forget(float rate) {
                return null;
            }

            @Override
            public float pri(@NotNull UDPeer.UDProfile key) {
                return (float)(1000.0 / (1000.0 + key.latency()));
            }

            @NotNull
            @Override
            public InetSocketAddress key(UDProfile value) {
                return value.addr;
            }
        };
        them.setCapacity(maxPeers);
    }

    /** broadcast */
    public void send(Object o, float pri) {

        if (them.isEmpty()) {
            System.err.println(this + " without any peers to broadcast");
        } else {

            byte[] bytes = json.toJson(o).getBytes(UTF8);

            them.sample((int) Math.ceil(pri * them.size()), (to) -> {
                outBytes(bytes, to.addr);
                return true;
            });
        }
    }

    /** send to a specific known recipient */
    public void send(Object o, InetSocketAddress to) {
        String s = json.toJson(o);
        byte[] bytes = s.getBytes(UTF8);

        outBytes(bytes, to);
    }

    @Override
    protected void in(byte[] data, InetSocketAddress from) {
        Map m = json.fromJson(new String(data), Map.class);
        receive(from, m);
    }

    public void ping(InetSocketAddress to) {
        Map p = Maps.mutable.of(
                "_", PING,
                "@", System.currentTimeMillis()
        );
        send(p, to);
    }

    public String summary() {
        return in + ", connected to " + them.size() + " peers";
    }

    public void receive(InetSocketAddress from, Map m) {

        long now = System.currentTimeMillis();

        System.out.println(this + " recv " + m + " from " + from + "(" + summary() + ")");

        @Nullable UDProfile connected = them.get(from);

        Object c = m.get("_");
        if (c == null) {
            //BAD
            return;
        }

        String cmd = c.toString();
        switch (cmd) {
            case PING:
                sendPong(from, m); //continue below
                break;
            case PONG:
                connected = recvPong(from, m, connected, now);
                return;
        }


        if (connected==null) {
            if (them.size() < them.capacity()) {
                //ping them to consider adding as peer
                ping(from);
            }
        } else {
            connected.lastMessage = now;
        }

        //handle payload
    }

    public @Nullable UDProfile recvPong(InetSocketAddress from, Map m, @Nullable UDProfile connected, long now) {
        Double sent = (Double) m.get("@"); //TODO should be Long
        long latency = now - Math.round(sent);
        if (connected != null) {
            connected.onPing(latency);
        } else {
            connected = them.put(new UDProfile(from, latency));
        }
        return connected;
    }

    protected void sendPong(InetSocketAddress from, Map m) {
        m.put("_", PONG); //change PING to PONG
        send(m, from);
    }


}
