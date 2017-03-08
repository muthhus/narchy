package jcog.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by me on 2/15/17.
 */
public class UDP  {

    /** in bytes */
    static final int MAX_PACKET_SIZE = 4096;

    static final int DEFAULT_socket_BUFFER_SIZE = 1024 * 1024;

    protected final DatagramSocket in;
    public final Thread thread;
    protected boolean running = true;
    private static final Logger logger = LoggerFactory.getLogger(UDP.class);

    private static final InetAddress local = new InetSocketAddress(0).getAddress();

    //final BidiMap<UUID,IntObjectPair<InetAddress>> who = new DualHashBidiMap<>();

    public final Gson json = new GsonBuilder()
            //.setLenient()
            //.disableHtmlEscaping()
            .create();

    public UDP(String host, int port) throws SocketException, UnknownHostException {
        this(InetAddress.getByName(host), port);
    }

    public UDP(int port) throws SocketException {
        this((InetAddress)null, port);
    }

    public UDP()  {
        DatagramSocket iin;
        try {
            iin = new DatagramSocket(null);
        } catch (SocketException e) {
            logger.error("{}", e);
            iin = null;
        }
        this.in = iin;
        this.thread = null;
    }

    public UDP(@Nullable InetAddress a, int port) throws SocketException {
        in = a!=null ? new DatagramSocket(port, a) : new DatagramSocket(port);
        in.setTrafficClass(0x10 /*IPTOS_LOWDELAY*/); //https://docs.oracle.com/javase/8/docs/api/java/net/DatagramSocket.html#setTrafficClass-int-
        in.setSendBufferSize(DEFAULT_socket_BUFFER_SIZE);
        in.setReceiveBufferSize(DEFAULT_socket_BUFFER_SIZE);

        this.thread = new Thread(this::recv);

        logger.info("{} started {} {}", this, in, in.getLocalSocketAddress());
        //logger.info("buffer sizes: send={} recv={}", in.getSendBufferSize(), in.getReceiveBufferSize());

        thread.start();
    }


    protected void recv() {
        byte[] receiveData = new byte[MAX_PACKET_SIZE];

        onStart();

        while (running) {
            try {
                DatagramPacket p = new DatagramPacket(receiveData, receiveData.length);
                in.receive(p);
                in(Arrays.copyOfRange(p.getData(), p.getOffset(), p.getLength()), (InetSocketAddress) p.getSocketAddress());
            } catch (Exception e) {
                logger.error("{}", e);
            }
        }
    }

    protected void onStart() {

    }

    public synchronized void stop() {
        if (running) {
            running = false;
            thread.stop();
        }
    }

    public boolean out(String data, String host, int port)  {
        try {
            return out(data.getBytes(), host, port);
        } catch (UnknownHostException e) {
            logger.error("{}", e.getMessage());
            return false;
        }
    }


    public boolean out(String data, int port) {
        return out(data.getBytes(), port);
    }

    public boolean out(byte[] data, int port) {
        return outBytes(data, new InetSocketAddress(local, port) );
    }

    public boolean out(byte[] data, String host, int port) throws UnknownHostException {
        return outBytes(data, new InetSocketAddress(InetAddress.getByName(host), port) );
    }

    public boolean outJSON(Object x, String host, int port) throws UnknownHostException {
        return outJSON(x, new InetSocketAddress(InetAddress.getByName(host), port)  );
    }

    public boolean outJSON(Object x, int port) throws UnknownHostException {
        return outJSON(x, new InetSocketAddress(local, port)  );
    }

    public boolean outJSON(Object x, InetSocketAddress addr)  {
        //DynByteSeq dyn = new DynByteSeq(MAX_PACKET_SIZE); //TODO wont work with the current hacked UTF output

//        ByteArrayDataOutput dyn = ByteStreams.newDataOutput();
//        json.toJson(x, new PrintStream());
//        return outBytes(dyn.array(), addr);

        String s = json.toJson(x);
        return outBytes(s.getBytes(UTF8), addr);

    }

    final static Charset UTF8 = Charset.forName("UTF8");


    final static ThreadLocal<DatagramPacket> packet = ThreadLocal.withInitial(()->{
        return new DatagramPacket(ArrayUtils.EMPTY_BYTE_ARRAY, 0, 0);
    });

    public boolean outBytes(byte[] data, InetSocketAddress to) {
        try {

            //DatagramPacket sendPacket = new DatagramPacket(data, data.length, to);

            DatagramPacket sendPacket = packet.get();
            sendPacket.setData(data, 0, data.length);
            sendPacket.setSocketAddress(to);
            in.send(sendPacket);
            return true;
        } catch (IOException e) {
            logger.error("{}", e);
            return false;
        }
    }

    protected void in(byte[] data, InetSocketAddress from) {

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
