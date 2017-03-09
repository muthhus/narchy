package jcog.net;

import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import jcog.bag.Bag;
import jcog.bag.PLink;
import jcog.bag.RawPLink;
import jcog.bag.impl.HijackBag;
import jcog.bag.impl.PLinkHijackBag;
import jcog.data.byt.DynByteSeq;
import jcog.random.XorShift128PlusRandom;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
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
    public static final byte SAY = (byte)'s';

    private static final byte DEFAULT_PING_TTL = 3;

    protected final InetSocketAddress me;



    static class Msg extends DynByteSeq {

        final static int TTL_BYTE = 0;
        final static int CMD_BYTE = 1;
        final static int PORT_BYTE = 2;
        final static int ADDR_BYTE = 4;
        final static int PAYLOAD_START_BYTE = 20;

        final static int HEADER_SIZE = PAYLOAD_START_BYTE /* ESTIMATE */;

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

        public int ttl() {
            return (int)bytes[TTL_BYTE];
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

        /** clones a new copy with different command */
        public Msg cmd(byte newCmd) {
            byte[] b = bytes.clone();
            b[CMD_BYTE] = newCmd;
            return new Msg(b);
        }

        public int dataLength() {
            return length() - PAYLOAD_START_BYTE;
        }

        public byte[] data(int start, int end) {
            return Arrays.copyOfRange(bytes, PAYLOAD_START_BYTE + start, PAYLOAD_START_BYTE + end );
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
    }

    /** profile of another peer */
    static class UDProfile {
        public final InetSocketAddress addr;

        final static int PING_WINDOW = 8;

        /** ping time, in ms */
        final SynchronizedDescriptiveStatistics pingTime = new SynchronizedDescriptiveStatistics(PING_WINDOW);

        long lastMessage = Long.MIN_VALUE;
        public byte[] addrBytes;


        public UDProfile(InetSocketAddress addr, long initialPingTime) {
            this.addr = addr;
            this.addrBytes = bytes(addr);
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

    public static byte[] bytes(InetSocketAddress addr) {
        return ArrayUtils.addAll(Shorts.toByteArray((short)addr.getPort()), addr.getAddress().getAddress());
    }

    /** max # of active links */
    final static int PEERS_CAPACITY = 8;

    /** message memory */
    final static int SEEN_CAPACITY = 4096;

    public final Bag<InetSocketAddress, UDProfile> them;
    public final PLinkHijackBag<Msg> seen;

    public UDPeer( int port) throws SocketException, UnknownHostException {
        super(InetAddress.getByName("[0:0:0:0:0:0:0:0]"), port);

        me = new InetSocketAddress(InetAddress.getByName("[0:0:0:0:0:0:0:0]"), port);

        XorShift128PlusRandom rng = new XorShift128PlusRandom(System.currentTimeMillis());

        them = new HijackBag<InetSocketAddress, UDProfile>(4, rng) {

            @Override
            public void onAdded(UDProfile p) {
                System.out.println(UDPeer.this + " connected " + p.addr + "( " + summary() + ")");
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
        them.setCapacity(PEERS_CAPACITY);

        seen = new PLinkHijackBag<>(SEEN_CAPACITY, 4, rng);
    }



    /** broadcast */
    public void send(Msg o, float pri) {

        if (them.isEmpty()) {
            //System.err.println(this + " without any peers to broadcast");
        } else {

            byte[] bytes = o.array();

            them.sample((int) Math.ceil(pri * them.size()), (to) -> {
                if (o.originEquals(to.addrBytes))
                    return false;

                outBytes(bytes, to.addr);
                return true;
            });
        }
    }

    public void say(String msg, byte ttl) {
        send(new Msg(SAY, ttl, me, msg.getBytes(UTF8) ), 1f);
    }

    /** send to a specific known recipient */
    public void send(Msg o, InetSocketAddress to) {
        outBytes(o.array(), to);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + me + ')';
    }

    @Override
    protected void in(byte[] data, InetSocketAddress from) {
        Msg m = Msg.get(data);
        if (m == null)
            return;

        float pri = 1f;

        PLink<Msg> pm = new RawPLink<>(m, pri);
        boolean seen = this.seen.put(pm)!=pm;
        if (seen)
            return;

        boolean continues = m.live();

        long now = System.currentTimeMillis();

        //System.out.println(this + " recv " + m + " from " + from + "(" + summary() + ")");

        @Nullable UDProfile connected = them.get(from);

        switch (m.cmd()) {
            case PONG:
                connected = recvPong(from, m, connected, now);
                continues = false;
                break;
            case PING:
                sendPong(from, m); //continue below
                break;
            case SAY:
                System.out.println( me + " recv: " + m.dataString() + " (ttl=" + m.ttl() + ")" );
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
            send(m, pri);
        }
    }

    public void ping(InetSocketAddress to) {
        send(new Msg(PING, DEFAULT_PING_TTL, me, System.currentTimeMillis()), to);
    }

    public String summary() {
        return in + ", connected to " + them.size() + " peers";
    }

    public @Nullable UDProfile recvPong(InetSocketAddress from, Msg m, @Nullable UDProfile connected, long now) {
        long sent = m.dataLong(); //TODO should be Long
        long latency = now - Math.round(sent);
        if (connected != null) {
            connected.onPing(latency);
        } else {
            connected = them.put(new UDProfile(from, latency));
        }
        return connected;
    }

    protected void sendPong(InetSocketAddress from, Msg ping) {
        send(ping.cmd(PONG), from);
    }

    public InetSocketAddress me() {
        return me;
    }

}
