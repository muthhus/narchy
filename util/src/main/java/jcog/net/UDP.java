package jcog.net;


import com.fasterxml.jackson.core.JsonProcessingException;
import jcog.Util;
import nars.util.Loop;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * generic UDP server & utilities
 */
public class UDP extends Loop {

    /** in bytes */
    static final int MAX_PACKET_SIZE = 1024;

    static final int DEFAULT_socket_BUFFER_SIZE = 1024 * 1024;

    protected final DatagramSocket in;

    public final Thread recvThread;

    final AtomicBoolean running = new AtomicBoolean();
    private static final Logger logger = LoggerFactory.getLogger(UDP.class);

    private static final InetAddress local = new InetSocketAddress(0).getAddress();

    private final int port;

    private final int updatePeriodMS = 250;



//    public UDP(String host, int port) throws SocketException, UnknownHostException {
//        this(InetAddress.getByName(host), port);
//    }

//    public UDP() {
//        DatagramSocket iin;
//        try {
//            iin = new DatagramSocket();
//        } catch (SocketException e) {
//            logger.error("{}", e);
//            iin = null;
//        }
//        this.in = iin;
//        this.recvThread = null;
//        this.port = -1;
//    }


    public UDP(@Nullable InetAddress a, int port) throws SocketException {
        super();
        in = a != null ? new DatagramSocket(port, a) : new DatagramSocket(port);
        //in.setTrafficClass(0x10 /*IPTOS_LOWDELAY*/); //https://docs.oracle.com/javase/8/docs/api/java/net/DatagramSocket.html#setTrafficClass-int-
        in.setSoTimeout(0);
        in.setSendBufferSize(DEFAULT_socket_BUFFER_SIZE);
        in.setReceiveBufferSize(DEFAULT_socket_BUFFER_SIZE);
        this.port = port;
        this.recvThread = new Thread(this::recv);
    }

    public UDP(int port) throws SocketException {
        this(null, port);
    }

    public int port() {
        return port;
    }


    public void start() {
        if (!running.compareAndSet(false, true))
            return;

        logger.info("{} start {} {} {} {}", this, in, in.getLocalSocketAddress(), in.getRemoteSocketAddress(), in.getInetAddress());

        setPeriodMS(updatePeriodMS);

        recvThread.start();

        onStart();
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("{} stop", this);
            super.stop();
            in.close();
            recvThread.interrupt();
        }
    }

    protected void recv() {

        while (running.get()) {
            try {

                byte[] receiveData = new byte[MAX_PACKET_SIZE];
                DatagramPacket p = new DatagramPacket(receiveData, receiveData.length);
                in.receive(p);

                in(p, receiveData);

            } catch (Throwable e) {
                if (!running.get() || !in.isClosed()) {
                    break;
                } else {
                    logger.warn("{}", e);
                }
            }
        }

        stop();

    }

    @Override public boolean next() {
        return true;
    }

    protected void onStart() {

    }



    @Deprecated
    public boolean out(String data, int port) {
        return out(data.getBytes(), port);
    }

    public boolean out(byte[] data, int port) {
        return outBytes(data, new InetSocketAddress(local, port));
    }

    public boolean out(byte[] data, String host, int port) throws UnknownHostException {
        return outBytes(data, new InetSocketAddress(InetAddress.getByName(host), port));
    }

    public boolean outJSON(Object x, int port) throws UnknownHostException {
        return outJSON(x, new InetSocketAddress(local, port));
    }

    public boolean outJSON(Object x, InetSocketAddress addr) {
        //DynByteSeq dyn = new DynByteSeq(MAX_PACKET_SIZE); //TODO wont work with the current hacked UTF output

//        ByteArrayDataOutput dyn = ByteStreams.newDataOutput();
//        json.toJson(x, new PrintStream());
//        return outBytes(dyn.array(), addr);


        byte[] b;
        try {
            b = Util.toBytes(x);
        } catch (JsonProcessingException e) {
            logger.error("{} ", e);
            return false;
        }

        return outBytes(b, addr);
    }

    final static Charset UTF8 = Charset.forName("UTF8");


//    final static ThreadLocal<DatagramPacket> packet = ThreadLocal.withInitial(() -> {
//        return new DatagramPacket(ArrayUtils.EMPTY_BYTE_ARRAY, 0, 0);
//    });

    public boolean outBytes(byte[] data, InetSocketAddress to) {

        DatagramPacket sendPacket = new DatagramPacket(data, data.length, to);
        //DatagramPacket sendPacket = packet.get();
        //sendPacket.setData(data, 0, data.length);
        //sendPacket.setSocketAddress(to);

        try {
            in.send(sendPacket);
            return true;
        } catch (IOException e) {
            logger.error("{} {}", in, e.getMessage());
            return false;
        }
    }

    protected void in(DatagramPacket p, byte[] data) {

    }

//    static class UDPClient {
//        public static void main(String args[]) throws Exception {
//            BufferedReader inFromUser =
//                    new BufferedReader(new InputStreamReader(System.in));
//            DatagramSocket clientSocket = new DatagramSocket();
//            InetAddress IPAddress = InetAddress.getByName("localhost");
//            byte[] sendData = new byte[1024];
//            byte[] receiveData = new byte[1024];
//            String sentence = inFromUser.readLine();
//            sendData = sentence.getBytes();
//            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
//            clientSocket.send(sendPacket);
//            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//            clientSocket.receive(receivePacket);
//            String modifiedSentence = new String(receivePacket.getData());
//            System.out.println("FROM SERVER:" + modifiedSentence);
//            clientSocket.close();
//        }
//    }
}


///** https://github.com/msgpack/msgpack-java */
//abstract public class ObjectUDP extends UDP {
//
//    private static final Logger logger = LoggerFactory.getLogger(ObjectUDP.class);
//
//
//
//    public ObjectUDP(String host, int port) throws SocketException, UnknownHostException {
//        super(host, port);
//    }
//
////
////    public boolean out(Object x, String host, int port)  {
////        try {
////            return out(toBytes(x), host, port);
////        } catch (IOException e) {
////            logger.error("{}", e);
////            return false;
////        }
////    }
//
////    protected byte[] toBytes(Object x) throws IOException {
////        return msgpack.write(x);
////    }
////
////    protected <X> byte[] toBytes(X x, Template<X> t) throws IOException {
////        return msgpack.write(x, t);
////    }
//
//
//    protected String stringFromBytes(byte[] x) {
//        try {
//            return MessagePack.newDefaultUnpacker(x).unpackString();
//        } catch (IOException e) {
//            logger.error("{}", e);
//            return null;
//        }
//
//        //Templates.tList(Templates.TString)
////        System.out.println(dst1.get(0));
////        System.out.println(dst1.get(1));
////        System.out.println(dst1.get(2));
////
////// Or, Deserialze to Value then convert type.
////        Value dynamic = msgpack.read(raw);
////        List<String> dst2 = new Converter(dynamic)
////                .read(Templates.tList(Templates.TString));
////        System.out.println(dst2.get(0));
////        System.out.println(dst2.get(1));
////        System.out.println(dst2.get(2));
//
//    }
//
//}
