package jcog.net;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import jcog.bag.Bag;
import jcog.bag.impl.HijackBag;
import jcog.bag.impl.hijack.PLinkHijackBag;
import jcog.byt.DynByteSeq;
import jcog.io.BinTxt;
import jcog.math.RecycledSummaryStatistics;
import jcog.pri.RawPLink;
import jcog.random.XorShift128PlusRandom;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * UDP peer
 *
 * see:
 *   Gnutella
 *   WASTE
 *   https://github.com/ethereum/ethereumj/blob/develop/ethereumj-core/src/main/java/org/ethereum/net/p2p/P2pMessageCodes.java
 *   https://github.com/ethereum/ethereumj/blob/develop/ethereumj-core/src/main/java/org/ethereum/net/shh/WhisperImpl.java
 *   https://github.com/ethereum/ethereumj/blob/develop/ethereumj-core/src/main/java/org/ethereum/net/MessageQueue.java
 */
public class UDPeer extends UDP {

    static {
        System.setProperty("java.net.preferIPv6Addresses", "true");
    }

    private static final Logger logger = LoggerFactory.getLogger(UDPeer.class);

    //TODO create a Command enum with interface for all stages of message handling
    public static final byte PING = (byte) 'P';
    public static final byte PONG = (byte) 'p';
    public static final byte WHO = (byte) 'w';
    public static final byte TELL = (byte) 's';

    private static final byte DEFAULT_PING_TTL = 2;

    //TODO use a 128+ bit identifier. ethereumj uses 512bits
    public final int id = ThreadLocalRandom.current().nextInt();


    /**
     * max # of active links
     * TODO make this IntParam mutable
     */
    final static int PEERS_CAPACITY = 16;

    /**
     * message memory
     */
    final static int SEEN_CAPACITY = 4096;


    public static class Msg extends DynByteSeq {

        final static int TTL_BYTE = 0;
        final static int CMD_BYTE = 1;
        final static int ID_BYTE = 2;
        final static int PORT_BYTE = 6;
        final static int ORIGIN_BYTE = 8;
        final static int DATA_START_BYTE = 24;

        final static int HEADER_SIZE = DATA_START_BYTE;

        final int hash;

        public Msg(byte... data) {
            super(data);

            hash = hash();
        }

        private void init(byte cmd, byte ttl, int id, InetSocketAddress origin) {
            writeByte(ttl);
            writeByte(cmd);
            writeInt(id);

            if (origin != null) {
                writeShort(origin.getPort());
                write(origin.getAddress().getAddress());
            } else {
                writeShort(0);
                for (int i = 0; i < ADDRESS_BYTES - 2; i++) //HACK
                    writeByte(0);
            }


        }

        public Msg(byte cmd, byte ttl, int id, InetSocketAddress origin, byte... payload) {
            super(HEADER_SIZE);
            init(cmd, ttl, id, origin);

            if (payload.length > 0)
                write(payload);

            hash = hash();
        }

        public Msg(byte cmd, byte ttl, int id, InetSocketAddress origin, int payload) {
            super(HEADER_SIZE);
            init(cmd, ttl, id, origin);

            writeInt(payload);

            hash = hash();
        }

        public Msg(byte cmd, byte ttl, int id, InetSocketAddress origin, long payload) {
            super(HEADER_SIZE);
            init(cmd, ttl, id, origin);

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
            Msg m = (Msg) obj;
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

        @Nullable
        public static Msg get(byte[] data) {
            //TODO verification
            return new Msg(data);
        }

        @Override
        public String toString() {

            return BinTxt.toString(id()).toString() + ' ' +
                    ((char)cmd()) + '+' + ttl() +
                    '[' + dataLength() + ']';

                    //origin() + ":" + ((char) cmd());
        }



        /**
         * clones a new copy with different command
         */
        public Msg clone(byte newCmd) {
            byte[] b = bytes.clone();
            b[CMD_BYTE] = newCmd;
            return new Msg(b);
        }

        public Msg clone(byte newCmd, @Nullable byte[] newOrigin) {
            byte[] b = bytes.clone();
            b[CMD_BYTE] = newCmd;

            if (newOrigin != null) {
                System.arraycopy(newOrigin, 0, b, PORT_BYTE, ADDRESS_BYTES);
            } else {
                Arrays.fill(b, PORT_BYTE, ADDRESS_BYTES, (byte) 0);
            }
            return new Msg(b);
        }

        public Msg clone(byte newCmd, int id, @Nullable byte[] newOrigin) {
            byte[] b = bytes.clone();
            b[CMD_BYTE] = newCmd;

            System.arraycopy(Ints.toByteArray(id), 0, b, ID_BYTE, 4);

            if (newOrigin != null) {
                System.arraycopy(newOrigin, 0, b, PORT_BYTE, ADDRESS_BYTES);
            } else {
                Arrays.fill(b, PORT_BYTE, ADDRESS_BYTES, (byte) 0);
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
            return Arrays.copyOfRange(bytes, DATA_START_BYTE + start, DATA_START_BYTE + end);
        }

        public int id() {
            byte[] b = bytes;
            return Ints.fromBytes(
                    b[ID_BYTE], b[ID_BYTE + 1], b[ID_BYTE + 2], b[ID_BYTE + 3]
            );
        }

        /**
         * the payload as a long
         */
        public long dataLong() {
            if (dataLength() != 8)
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

        final static int ADDRESS_BYTES = 16 /* ipv6 */ +2 /* port */;

        public void dataAddresses(Consumer<InetSocketAddress> a) {
            int d = dataLength();
            if (d % ORIGIN_BYTE != 0)
                return; //corrupt

            int addresses = d / ADDRESS_BYTES;
            int o = DATA_START_BYTE;
            for (int i = 0; i < addresses; i++) {
                byte[] addr = Arrays.copyOfRange(bytes, o, o + 16);
                try {
                    InetAddress aa = InetAddress.getByAddress(addr);
                    int port = Shorts.fromBytes(bytes[o + 16], bytes[o + 17]);
                    a.accept(new InetSocketAddress(aa, port));
                } catch (UnknownHostException e) {
                    continue;
                }
                o += ADDRESS_BYTES;
            }

        }

        @Nullable
        public InetSocketAddress origin() {
            int port = Shorts.fromBytes(bytes[PORT_BYTE], bytes[PORT_BYTE + 1]);
            InetAddress aa = null;
            try {
                aa = InetAddress.getByAddress(Arrays.copyOfRange(bytes, ORIGIN_BYTE, ORIGIN_BYTE + 16));
                return new InetSocketAddress(aa, port);
            } catch (UnknownHostException e) {
                return null;
            }

        }

        public int port() {
            return Shorts.fromBytes(bytes[PORT_BYTE], bytes[PORT_BYTE + 1]);
        }
    }

    /**
     * profile of another peer
     */
    static class UDProfile {
        public final InetSocketAddress addr;

        final static int PING_WINDOW = 8;

        /**
         * ping time, in ms
         */
        final SynchronizedDescriptiveStatistics pingTime = new SynchronizedDescriptiveStatistics(PING_WINDOW);
        //private final int id;

        long lastMessage = Long.MIN_VALUE;
        public byte[] addrBytes;
        private long latency;


        public UDProfile(int id, InetSocketAddress addr, long initialPingTime) {
            //this.id = id;
            this.addr = addr;
            this.addrBytes = bytes(addr);
            onPing(initialPingTime);
        }

        @Override
        public int hashCode() {
            return addr.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return addr.equals(((UDProfile) obj).addr);
        }

        public void onPing(long time) {
            pingTime.addValue(time);
            latency = Math.round(pingTime.getMean());
        }

        /**
         * average ping time in ms
         */
        public long latency() {
            return latency;
        }

        @Override
        public String toString() {
            return addr + " (latency=" + latency() + ")";
        }
    }

    public static byte[] bytes(InetSocketAddress addr) {

        return ArrayUtils.addAll(Shorts.toByteArray((short) addr.getPort()),
                ipv6(addr.getAddress().getAddress()));

    }

    private static byte[] ipv6(byte[] address) {
        if (address.length == 4) {
            byte ipv4asIpV6addr[] = new byte[16];
            ipv4asIpV6addr[10] = (byte) 0xff;
            ipv4asIpV6addr[11] = (byte) 0xff;
            ipv4asIpV6addr[12] = address[0];
            ipv4asIpV6addr[13] = address[1];
            ipv4asIpV6addr[14] = address[2];
            ipv4asIpV6addr[15] = address[3];
            return ipv4asIpV6addr;
        } else {
            return address;
        }
    }


    public final Bag<InetSocketAddress, UDProfile> them;
    public final PLinkHijackBag<Msg> seen;

    public UDPeer(int port) throws SocketException {
        super(port);
        //super( InetAddress.getLocalHost().getCanonicalHostName(), port);

        //this.me =  new InetSocketAddress( in.getInetAddress(), port );
        /*this.me = new InetSocketAddress(
                InetAddress.getByName("[0:0:0:0:0:0:0:0]"),
                port);*/
        //this.meBytes = bytes(me);

        XorShift128PlusRandom rng = new XorShift128PlusRandom(System.currentTimeMillis());

        them = new HijackBag<InetSocketAddress, UDProfile>(4) {

            @Override
            public void onAdded(UDProfile p) {
                logger.debug("{} connect {}", UDPeer.this, p);
                UDPeer.this.onAddRemove(p, true);
            }

            @Override
            public void onRemoved(@NotNull UDPeer.UDProfile p) {
                logger.debug("{} disconnect {}", UDPeer.this, p);
                UDPeer.this.onAddRemove(p, false);
            }

            @Override
            protected UDPeer.UDProfile merge(@Nullable UDPeer.UDProfile existing, @NotNull UDPeer.UDProfile incoming, float scale) {
                return (existing!=null ? existing : incoming);
            }

            @Override
            protected Consumer<UDProfile> forget(float rate) {
                return null;
            }

            @Override
            public float pri(@NotNull UDPeer.UDProfile key) {
                return 1f / (1f + key.latency() / 20f);
            }

            @NotNull
            @Override
            public InetSocketAddress key(UDProfile value) {
                return value.addr;
            }

        };

        them.setCapacity(PEERS_CAPACITY);

        seen = new PLinkHijackBag<>(SEEN_CAPACITY, 4);
    }

    protected void onAddRemove(UDProfile p, boolean addedOrRemoved) {

    }


    /**
     * broadcast
     *
     * @return how many sent
     */
    public int say(Msg o, float pri, boolean onlyIfNotSeen) {

        if (them.isEmpty()) {
            //System.err.println(this + " without any peers to broadcast");
            return 0;
        } else {

            if (onlyIfNotSeen && seen(o, pri))
                return 0;

            byte[] bytes = o.array();

            final int[] count = {0};
            them.sample((int) Math.ceil(pri * them.size()), (to) -> {
                //logger.debug("({} =/> {})", o, to.addr);
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
        return say(new Msg(TELL, (byte) ttl, id, null, msg), 1f, onlyIfNotSeen);
    }

    /**
     * send to a specific known recipient
     */
    public void send(Msg o, InetSocketAddress to) {
//        InetSocketAddress a = o.origin();
//        if (a != null && a.equals(to))
//            return;
        outBytes(o.array(), to);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + BinTxt.toString(id) + ')';
    }

    @Override
    protected void in(DatagramPacket p, byte[] data) {
        Msg m = Msg.get(data);
        if (m == null)
            return;

        float pri = 1;
        boolean seen = seen(m, pri);
        if (seen)
            return;

        byte cmd = m.cmd();
        if (m.port() == 0) {
            //rewrite origin with the actual packet origin
            m = m.clone(cmd, bytes(new InetSocketAddress(p.getAddress(), p.getPort())));
        }

        boolean continues = m.live();

        long now = System.currentTimeMillis();

        @Nullable UDProfile connected = them.get(p.getSocketAddress());

        switch (cmd) {
            case PONG:
                connected = recvPong(p, m, connected, now);
                return;
                //continues = false;
                //break;
        }

        if (m.id() == id) {
            return;
        }

        InetSocketAddress remote = (InetSocketAddress) p.getSocketAddress();

        switch (cmd) {
            case PING:
                sendPong(remote, m); //continue below
                break;
            case WHO:
                m.dataAddresses(this::ping);
                break;
            case TELL:
                //System.out.println(me + " recv: " + m.dataString() + " (ttl=" + m.ttl() + ")");
                receive(m);
                break;
            default:
                return;
        }


        if (connected == null) {
            if (them.size() < them.capacity()) {
                //ping them to consider adding as peer
                ping(remote);
            }
        } else {
            connected.lastMessage = now;
        }


        //direct bounce:
            //if (continues) {
            //    if (!seen)
            //        send(m, pri, false /* did a test locally already */);
            //}
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

    public @Nullable UDProfile recvPong(DatagramPacket p, Msg m, @Nullable UDProfile connected, long now) {

        long latency = pingTime(m, now); //TODO should be Long
        if (connected != null) {
            connected.onPing(latency);
        } else {
            if (id == m.id())
                connected = them.put(new UDProfile(-1, (InetSocketAddress) p.getSocketAddress(), latency));
        }
        return connected;
    }

    private long pingTime(Msg m, long now) {
        long sent = m.dataLong(); //TODO dont store the sent time in the message where it can be spoofed. instead store a pending ping table that a pong will lookup by the iniating ping's message hash
        return now - sent;
    }

    /**
     * ping same host, different port
     */
    public void ping(int port) {
        ping(new InetSocketAddress(port));
    }

    public void ping(String host, int port) {
        ping(new InetSocketAddress(host, port));
    }

    public void ping(@Nullable InetSocketAddress to) {
        send(new Msg(PING, DEFAULT_PING_TTL, id, null, System.currentTimeMillis()), to);
    }


    protected void sendPong(InetSocketAddress from, Msg ping) {
        Msg p = ping.clone(PONG,null);

        //logger.debug("({} =/> {})", p, from);

        send(p, from);
    }

}
