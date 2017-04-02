package jcog.net;

import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import jcog.bag.Bag;
import jcog.bag.RawPLink;
import jcog.bag.impl.HijackBag;
import jcog.bag.impl.PLinkHijackBag;
import jcog.data.byt.DynByteSeq;
import jcog.math.RecycledSummaryStatistics;
import jcog.random.XorShift128PlusRandom;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.*;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * UDP peer
 */
public class UDPeer extends UDP {

    static {
        System.setProperty("java.net.preferIPv6Addresses", "true");
    }

    public static final byte PING = (byte)'P';
    public static final byte PONG = (byte)'p';
    public static final byte WHO = (byte)'w';
    public static final byte SAY = (byte)'s';

    private static final byte DEFAULT_PING_TTL = 3;

    protected final InetSocketAddress me;
    private final byte[] meBytes;

    /** max # of active links */
    final static int PEERS_CAPACITY = 4;

    /** message memory */
    final static int SEEN_CAPACITY = 4096;


    public static class Msg extends DynByteSeq {

        final static int TTL_BYTE = 0;
        final static int CMD_BYTE = 1;
        final static int PORT_BYTE = 2;
        final static int ORIGIN_BYTE = 4;
        final static int DATA_START_BYTE = 20;

        final static int HEADER_SIZE = DATA_START_BYTE /* ESTIMATE */;

        final int hash;

        public Msg(byte... data) {
            super(data);

            hash = hash();
        }

        private void init(byte cmd, byte ttl, InetSocketAddress origin) {
            write(ttl);
            write(cmd);

            writeShort(origin.getPort());
            write(origin.getAddress().getAddress());
        }

        public Msg(byte cmd, byte ttl, InetSocketAddress origin, byte... payload) {
            super(HEADER_SIZE);
            init(cmd, ttl, origin);

            if (payload.length > 0)
                write(payload);

            hash = hash();
        }

        public Msg(byte cmd, byte ttl, InetSocketAddress origin, int payload) {
            super(HEADER_SIZE);
            init(cmd, ttl, origin);

            writeInt(payload);

            hash = hash();
        }

        public Msg(byte cmd, byte ttl, InetSocketAddress origin, long payload) {
            super(HEADER_SIZE);
            init(cmd, ttl, origin);

            writeLong(payload);

            hash = hash();
        }


        private int hash() {
            compact();
            return hash(1  /* skip TTL byte */, len);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            Msg m = (Msg)obj;
            return (m.hash == hash) && m.bytes.length == bytes.length && Arrays.equals(m.bytes, 1, len, bytes, 1, len);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        public byte cmd() {
            return bytes[CMD_BYTE];
        }

        public byte ttl() {
            return bytes[TTL_BYTE];
        }

        public boolean live() {
            int ttl = ttl();
            if (ttl <= 0)
                return false;
            return (--bytes[TTL_BYTE]) >= 0;
        }

        @Nullable public static Msg get(byte[] data) {
            //TODO verification
            return new Msg(data);
        }

        @Override
        public String toString() {
            return origin() + ":" + ((char)cmd());
        }

        /** clones a new copy with different command */
        public Msg cmd(byte newCmd) {
            byte[] b = bytes.clone();
            b[CMD_BYTE] = newCmd;
            return new Msg(b);
        }
        public Msg cmd(byte newCmd, @Nullable byte[] newOrigin) {
            byte[] b = bytes.clone();
            b[CMD_BYTE] = newCmd;
            if (newOrigin != null) {
                System.arraycopy(newOrigin, 0, b, PORT_BYTE, ADDRESS_BYTES);
            } else {
                Arrays.fill(b, PORT_BYTE, ADDRESS_BYTES, (byte)0);
            }
            return new Msg(b);
        }

        public int dataLength() {
            return length() - DATA_START_BYTE;
        }

        public byte[] data() {
            return data(0, dataLength());
        }

        public byte[] data(int start, int end) {
            return Arrays.copyOfRange(bytes, DATA_START_BYTE + start, DATA_START_BYTE + end );
        }

        /** the payload as a long */
        public long dataLong() {
            if (dataLength() != 8 )
                throw new RuntimeException("unexpected payload");

            return Longs.fromByteArray(data(0, 8));
        }

        public boolean originEquals(byte[] addrBytes) {
            int addrLen = addrBytes.length;
            return Arrays.equals(bytes, PORT_BYTE, PORT_BYTE + addrLen, addrBytes, 0, addrLen);
        }

        public String dataString() {
            return new String(data(0, dataLength()));
        }

        final static int ADDRESS_BYTES = 18;

        public void dataAddresses(Consumer<InetSocketAddress> a) {
            int d = dataLength();
            if (d % ORIGIN_BYTE !=0)
                return; //corrupt

            int addresses = d / ADDRESS_BYTES;
            int o = DATA_START_BYTE;
            for (int i = 0; i < addresses; i++) {
                byte[] addr = Arrays.copyOfRange(bytes, o, o+16);
                try {
                    InetAddress aa = InetAddress.getByAddress(addr);
                    int port = Shorts.fromBytes(bytes[o+16], bytes[o+17]);
                    a.accept(new InetSocketAddress(aa, port));
                } catch (UnknownHostException e) {
                    continue;
                }
                o += ADDRESS_BYTES;
            }

        }

        @Nullable public InetSocketAddress origin() {
            int port = Shorts.fromBytes(bytes[PORT_BYTE], bytes[PORT_BYTE+1]);
            InetAddress aa = null;
            try {
                aa = InetAddress.getByAddress(Arrays.copyOfRange(bytes, ORIGIN_BYTE, ORIGIN_BYTE+16));
                return new InetSocketAddress(aa, port);
            } catch (UnknownHostException e) {
                return null;
            }

        }
    }

    /** profile of another peer */
    static class UDProfile {
        public final InetSocketAddress addr;

        final static int PING_WINDOW = 8;

        /** ping time, in ms */
        final SynchronizedDescriptiveStatistics pingTime = new SynchronizedDescriptiveStatistics(PING_WINDOW);

        long lastMessage = Long.MIN_VALUE;
        public byte[] addrBytes;
        private long latency;


        public UDProfile(InetSocketAddress addr, long initialPingTime) {
            this.addr = addr;
            this.addrBytes = bytes(addr);
            onPing(initialPingTime);
        }

        public void onPing(long time) {
            pingTime.addValue(time);
            latency = Math.round(pingTime.getMean());
        }

        /** average ping time in ms */
        public long latency() {
            return latency;
        }

        @Override
        public String toString() {
            return addr + " (latency=" + latency() + ")";
        }
    }

    public static byte[] bytes(InetSocketAddress addr) {
        return ArrayUtils.addAll(Shorts.toByteArray((short)addr.getPort()), addr.getAddress().getAddress());
    }


    public final Bag<InetSocketAddress, UDProfile> them;
    public final PLinkHijackBag<Msg> seen;

    public UDPeer( int port) throws SocketException, UnknownHostException {
        super(java.net.InetAddress.getLocalHost().getHostName(), port);

        this.me =  new InetSocketAddress( in.getInetAddress(), port );
        /*this.me = new InetSocketAddress(
                InetAddress.getByName("[0:0:0:0:0:0:0:0]"),
                port);*/

        this.meBytes = bytes(me);

        XorShift128PlusRandom rng = new XorShift128PlusRandom(System.currentTimeMillis());

        them = new HijackBag<InetSocketAddress, UDProfile>(4, rng) {

            @Override
            public void onAdded(UDProfile p) {
                System.out.println(UDPeer.this + " connected " + p + "( " + summary() + ")");
            }

            @Override
            public void onRemoved(@NotNull UDPeer.UDProfile p) {
                System.out.println(UDPeer.this + " disconnected " + p );
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
                return (float)(1f / (1f + key.latency()/20f));
            }

            @NotNull
            @Override
            public InetSocketAddress key(UDProfile value) {
                return value.addr;
            }
        };
        them.setCapacity(PEERS_CAPACITY);

        seen = new PLinkHijackBag<>(SEEN_CAPACITY, 4, rng);
    }



    /** broadcast
     * @return how many sent
     * */
    public int send(Msg o, float pri, boolean onlyIfNotSeen) {

        if (them.isEmpty()) {
            //System.err.println(this + " without any peers to broadcast");
            return 0;
        } else {

            if (onlyIfNotSeen && seen(o, pri))
                return 0;

            byte[] bytes = o.array();

            final int[] count = {0};
            them.sample((int) Math.ceil(pri * them.size()), (to) -> {
                if (o.originEquals(to.addrBytes))
                    return false;

                outBytes(bytes, to.addr);
                count[0]++;
                return true;
            });
            return count[0];
        }
    }

    public boolean seen(Msg o, float pri) {
        RawPLink p = new RawPLink(o, pri);
        return seen.put(p) != p; //what about if it returns null
    }

    public void say(String msg, int ttl) {
        say(msg.getBytes(UTF8), ttl);
    }

    public void say(byte[] msg, int ttl) {
        say(msg, ttl, false);
    }

    public int say(byte[] msg, int ttl, boolean onlyIfNotSeen) {
        return send(new Msg(SAY, (byte)ttl, me, msg ), 1f, onlyIfNotSeen);
    }

    /** send to a specific known recipient */
    public void send(Msg o, InetSocketAddress to) {
        InetSocketAddress a = o.origin();
        if (a != null && a.equals(to))
            return;
        outBytes(o.array(), to);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + me + ')';
    }

    @Override
    protected void in(DatagramPacket p, byte[] data, InetSocketAddress from) {
        Msg m = Msg.get(data);
        if (m == null)
            return;

        if (m.origin()==null) {
            //rewrite origin with the actual packet origin
            m = m.cmd(m.cmd(), bytes(new InetSocketAddress(p.getAddress(), p.getPort())) );
        }

        if (m.originEquals(meBytes)) {
            //throw new RuntimeException("received by originator");
            return;
        }

        float pri = 1;

        boolean seen = seen(m, pri);
        if (seen)
            return;

        boolean continues = m.live();

        long now = System.currentTimeMillis();

        //System.out.println(this + " recv " + m + " from " + from + "(" + summary() + ")");

        @Nullable UDProfile connected = them.get(from);

        switch (m.cmd()) {
            case PONG:
                connected = recvPong(m, connected, now);
                //continues = false;
                break;
            case PING:
                sendPong(from, m); //continue below
                break;
            case WHO:
                m.dataAddresses(this::ping);
                break;
            case SAY:
                //System.out.println(me + " recv: " + m.dataString() + " (ttl=" + m.ttl() + ")");
                receive(m);
                break;
            default:
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

        if (continues) {
            send(m, pri, false);
        }
    }

    protected void receive(Msg m) {

    }


    public long latencyAvg() {
        RecycledSummaryStatistics r = new RecycledSummaryStatistics();
        them.forEach(x -> r.accept(x.latency));
        return Math.round(r.getMean());
    }

    public String summary() {
        return in + ", connected to " + them.size() + " peers, (avg latency=" + latencyAvg() + ")";
    }

    public @Nullable UDProfile recvPong(Msg m, @Nullable UDProfile connected, long now) {

        long sent = m.dataLong(); //TODO should be Long
        long latency = now - (sent);
        if (connected != null) {
            connected.onPing(latency);
        } else {
            InetSocketAddress origin = m.origin();
            connected = them.put(new UDProfile(origin, latency));
        }
        return connected;
    }

    /** ping same host, different port */
    public void ping(int port) {
        ping(new InetSocketAddress(me.getAddress(), port));
    }

    public void ping(String host, int port) {
        throw new UnsupportedOperationException("TODO");
    }

    public void ping(@Nullable InetSocketAddress to) {
        if (to!=null && to.equals(me))
            return;
        send(new Msg(PING, DEFAULT_PING_TTL, null, System.currentTimeMillis()), to);
    }


    protected void sendPong(InetSocketAddress from, Msg ping) {
        send(ping.cmd(PONG, null), from);
    }

    public InetSocketAddress me() {
        return me;
    }

}
