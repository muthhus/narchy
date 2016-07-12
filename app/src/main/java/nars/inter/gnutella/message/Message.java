package nars.inter.gnutella.message;

import ch.qos.logback.core.encoder.ByteArrayUtil;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import infinispan.com.mchange.util.ByteArrayComparator;
import nars.inter.gnutella.GnutellaConstants;
import nars.inter.gnutella.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * Class that represents the structure of the header of a Message specified in
 * the Gnutella Protocol v0.4
 *
 * @author Ismael Fernandez
 * @author Miguel Vilchis
 * @version 2.0
 * @see
 */

public abstract class Message implements Comparable<Message> {

    public static final Logger logger = LoggerFactory.getLogger(Message.class);

    public final byte[] id;
    public byte type;
    public byte ttl;
    @Deprecated public byte hop;
    public final InetSocketAddress origin;

    @Override
    public int compareTo(Message o) {
        return Arrays.compare(id, o.id); //TODO may not be enough if message id's are re-used for replies etc
    }

    /**
     * Creates a header used on Gnutella Protocol v0.4
     *
     * @param id           A 16-byte string uniquely identifying the descriptor on the
     *                     network
     * @param type     0x00 = Ping, 0x01 = Pong, 0x40 = Push, 0x80 = Query, 0x81 =
     *                     QueryHit PayLoader Descriptor
     * @param ttl          Time to live. The number of times the descriptor will be
     *                     forwarded by Gnutella servents before it is removed from the
     *                     network
     * @param hop          The number of times the descriptor has been forwarded
     * @param origin Id of the thread that received the message
     */
    protected Message(byte[] id, byte type, byte ttl, byte hop,
                      InetSocketAddress origin) {
        this.id = id!=null ? id  : IdGenerator.next();
        this.type = type;
        this.ttl = ttl;
        this.hop = hop;
        this.origin = origin;

    }

    /**
     * @param idMessage    A 16-byte string uniquely identifying the descriptor on the
     *                     network
     * @param type     0x00 = Ping, 0x01 = Pong, 0x40 = Push, 0x80 = Query, 0x81 =
     *                     QueryHit PayLoader Descriptor
     * @param ttl          Time to live. The number of times the descriptor will be
     *                     forwarded by Gnutella servents before it is removed from the
     *                     network
     * @param hop          The number of times the descriptor has been forwarded
     * @param payloadL     The length of the descriptor immediately following this header
     * @param origin thread that received the message
     */
    protected Message(byte type, byte ttl, byte hop,
                      InetSocketAddress origin) {
        this(null, type, ttl, hop, origin);
    }

    public Message(byte type, ByteArrayDataInput in, InetSocketAddress origin) {
        this.type = type;
        this.origin = origin;
        this.id = new byte[GnutellaConstants.ID_LENGTH];
        in(in);
    }

    /**
     * Construct a Message read in the given DataInputStream. The message is
     * read byte by byte
     *
     * @param inStream DataInputStream in which the Message is read in bytes
     * @return Message of the Gnutella Protocol v0.4
     */
    public static Message nextMessage(byte type, ByteArrayDataInput in, InetSocketAddress origin)  {

        switch (type) {
            case GnutellaConstants.PING:
                return new PingMessage(in, origin);
            case GnutellaConstants.PONG:
                return new PongMessage(in, origin);
            case GnutellaConstants.QUERY:
                return new QueryMessage(in, origin);
            default:
                //TODO remaining types
                return null;
        }

//        ByteArrayList message = new ByteArrayList();
//        int idx = 0;
//
//
//        while (idx < GnutellaConstants.HEADER_LENGTH) {
//
//            message.add(inStream.readByte()); //blocks for input here
//            idx++;
//
//        }
//            /* Declaracion de lo que almacenara el header del message */
//        byte[] idMessage = new byte[GnutellaConstants.ID_LENGTH];
//        int j = 0;
//
//        byte stream[] = message.toArray();
//
//        // Leemos el id
//        for (int i = 0; i < GnutellaConstants.ID_LENGTH; i++) {
//            idMessage[i] = stream[j++];
//        }
//        // Leemos el payload descriptor
//
//        byte payloadD = stream[j++];
//
//        // Leemos el ttl
//        byte ttl = stream[j++];
//
//        // Leemos el hop
//        byte hop = stream[j++];
//
//        // Leemos el payload length
//        byte[] payloadL = new byte[GnutellaConstants.PLL_LENGTH];
//        for (int i = 0; i < GnutellaConstants.PLL_LENGTH; i++) {
//            payloadL[i] = stream[j++];
//        }
//        switch (payloadD) {
//            case GnutellaConstants.PING:
//                return new PingMessage(idMessage, ttl, hop, origin);
//
//            case GnutellaConstants.PONG:
//                ByteArrayList partPong = new ByteArrayList();
//                while (inStream.available() > 0
//                        && idx < GnutellaConstants.HEADER_LENGTH
//                        + GnutellaConstants.PONG_PLL) {
//                    partPong.add(inStream.readByte());
//                    idx++;
//                }
//
//                stream = partPong.toArray();
//                // Declaracion de donde almacenaremos los atributos del mensaje
//                // pong
//                j = 0;
//                // Llenamos cada campo con lo que habia en el stream
//                byte[] port = new byte[GnutellaConstants.PORT_LENGTH];
//                for (int i = 0; i < GnutellaConstants.PORT_LENGTH; i++) {
//                    port[i] = stream[j++];
//                }
//                byte[] ip = new byte[GnutellaConstants.IP_LENGTH];
//                for (int i = 0; i < GnutellaConstants.IP_LENGTH; i++) {
//                    ip[i] = stream[j++];
//                }
//                byte[] nfilesh = new byte[GnutellaConstants.NF_LENGTH];
//                for (int i = 0; i < GnutellaConstants.NF_LENGTH; i++) {
//                    nfilesh[i] = stream[j++];
//                }
//
//                byte[] nkbsh = new byte[GnutellaConstants.NK_LENGTH];
//                for (int i = 0; i < GnutellaConstants.NK_LENGTH; i++) {
//                    nkbsh[i] = stream[j++];
//                }
//                /* Convertimos el arreglo de byte del puerto a short */
//
//                return new PongMessage(idMessage, ttl, hop, origin, port,
//                        ip, nfilesh, nkbsh);
//            case GnutellaConstants.QUERY:
//
//                ByteArrayList partQuery = new ByteArrayList();
//
//                while (inStream.available() > 0) {
//                    partQuery.add(inStream.readByte());
//                    idx++;
//                }
//
//                stream = partQuery.toArray();
//                // Declaracion de donde almacenaremos los atributos del mensaje
//                // pong
//
//                int searchCriteriaL = partQuery.size()
//                        - GnutellaConstants.MINSPEEDL - GnutellaConstants.EOS_L;
//
//                j = 0;
//                // Llenamos cada campo con lo que habia en el stream
//                byte[] minSpeed = new byte[GnutellaConstants.MINSPEEDL];
//                for (int i = 0; i < GnutellaConstants.MINSPEEDL; i++) {
//                    minSpeed[i] = stream[j++];
//                }
//                byte[] searchCriteria = new byte[searchCriteriaL];
//                for (int i = 0; i < searchCriteriaL; i++) {
//                    searchCriteria[i] = stream[j++];
//                }
//                return new QueryMessage(idMessage, ttl, hop, searchCriteriaL,
//                        origin, minSpeed, new String(searchCriteria));
//
//            case GnutellaConstants.QUERY_HIT:
//                ByteArrayList partQueryH = new ByteArrayList();
//
//                while (inStream.available() > 0) {
//                    byte b = inStream.readByte();
//                    partQueryH.add(b);
//
//                    idx++;
//                }
//
//                stream = partQueryH.toArray();
//
//                // Declaracion de donde almacenaremos los atributos del mensaje
//                // pong
//                j = 0;
//                //byte nHits = stream[j++];
//
//                byte[] portQ = new byte[GnutellaConstants.PORT_LENGTH];
//                //byte[][] fIQ = new byte[nHits][4];
//                //byte[][] fSQ = new byte[nHits][4];
//
//                //int payloadSize = partQueryH.size();
//                    /*int resultL = payloadSize - //nHits
//                            //* GnutellaConstants.QUERYHIT_PART_L
//                            - GnutellaConstants.SERVER_ID_L;*/
//
//                //byte[][] name = new byte[nHits][nameL];
//
//                // Llenamos cada campo con lo que habia en el stream
//                for (int i = 0; i < GnutellaConstants.PORT_LENGTH; i++) {
//                    portQ[i] = stream[j++];
//                }
//                byte[] ipQ = new byte[GnutellaConstants.IP_LENGTH];
//                for (int i = 0; i < GnutellaConstants.IP_LENGTH; i++) {
//                    ipQ[i] = stream[j++];
//                }
//
//                byte[] speedQ = new byte[4];
//                for (int i = 0; i < 4; i++) {
//                    speedQ[i] = stream[j++];
//                }
////                    for (int k = 0; k < nHits; k++) {
////
////                        for (int i = 0; i < 4; i++) {
////                            fIQ[k][i] = stream[j++];
////                        }
////                        for (int i = 0; i < 4; i++) {
////                            fSQ[k][i] = stream[j++];
////                        }
////
////                        for (int i = 0; i < nameL; i++) {
////                            if (stream[j] == GnutellaConstants.END) {
////                                j++;
////                                break;
////                            }
////                            name[k][i] = stream[j++];
////                        }
////
////                    }
//                int payload = stream.length - j - GnutellaConstants.SERVER_ID_L;
//
//
//                byte[] result = new byte[payload];
//                for (int i = 0; i < payload; i++) {
//                    result[i] = stream[j++];
//                }
//                byte[] idServent = new byte[GnutellaConstants.SERVER_ID_L];
//                for (int i = 0; i < GnutellaConstants.SERVER_ID_L; i++) {
//                    idServent[i] = stream[j++];
//                }
//
//
//                return new QueryHitMessage(idMessage, ttl, hop,
//                        stream.length, origin, portQ,
//                        InetAddress.getByAddress(ipQ), speedQ, result,
//                        idServent);
//
//            case GnutellaConstants.PUSH:
//                return null;
//
//            default:
//                Message.logger.warn("unknown message type {} ", payloadD);
//
//        }
//        return null;

    }

    public boolean refreshMessage() {
        // Hop se incializa en -1 si nosotros creamos el mensaje
        if (hop == GnutellaConstants.MY_MESSAGE) {
            hop++;
            return true;
        }
        if (ttl > 0) {
            hop++;
            ttl--;
            return true;
        }
        return false;
    }


    public final void in(ByteArrayDataInput in) {
        //NOTE: the initial type byte should already have been read and known
        //NOTE: the size short (16 bits) should have already been read and known
        in.readFully(id);
        ttl = in.readByte();
        hop = in.readByte();
        inData(in);
    }

    protected final void out(ByteArrayDataOutput out) {

        out.writeByte(type);
        out.writeShort(0); //size, will be filled in after the message has been construced
        out.write(id);
        out.writeByte(ttl);
        out.writeByte(hop);
        outData(out);

    }

    abstract protected void inData(ByteArrayDataInput in);
    abstract protected void outData(ByteArrayDataOutput out);


    /**
     * Returns the id of this Message in textual presentation
     *
     * @return the id in a string format
     */
    public String idString() {
        return new BigInteger(id).toString(36);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {

        return getClass().getSimpleName() + '|' + idString() + '|' + type + '|' + ttl
                + '|' + hop;
    }


    final static int ESTIMATED_MESSAGE_SIZE = 512; //in bytes

    public byte[] asBytes() {
        ByteArrayDataOutput oo = ByteStreams.newDataOutput(ESTIMATED_MESSAGE_SIZE);
        out(oo);
        byte[] x = oo.toByteArray();
        short size = (short)(x.length - 3);
        //add the size information in bytes 1 and 2
        x[1] = (byte)(size & 0x00ff);   //lower 8 bits of the size
        x[2] = (byte)((size & 0xff00) >> 8); //upper 8 bits of the size
        return x;
    }

}
