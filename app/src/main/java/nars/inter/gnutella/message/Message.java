package nars.inter.gnutella.message;

import com.gs.collections.impl.list.mutable.primitive.ByteArrayList;
import nars.inter.gnutella.GnutellaConstants;
import nars.inter.gnutella.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Class that represents the structure of the header of a Message specified in
 * the Gnutella Protocol v0.4
 *
 * @author Ismael Fernandez
 * @author Miguel Vilchis
 * @version 2.0
 * @see
 */

public class Message {

    public static final Logger logger = LoggerFactory.getLogger(Message.class);

    public final BigInteger id;
    public final byte payloadD;
    byte ttl;
    byte hop;
    public final int payloadL;
    public final InetSocketAddress recipient;

    /**
     * Creates a header used on Gnutella Protocol v0.4
     *
     * @param id           A 16-byte string uniquely identifying the descriptor on the
     *                     network
     * @param payloadD     0x00 = Ping, 0x01 = Pong, 0x40 = Push, 0x80 = Query, 0x81 =
     *                     QueryHit PayLoader Descriptor
     * @param ttl          Time to live. The number of times the descriptor will be
     *                     forwarded by Gnutella servents before it is removed from the
     *                     network
     * @param hop          The number of times the descriptor has been forwarded
     * @param payloadL     The length of the descriptor immediately following this header
     * @param recipient Id of the thread that received the message
     */
    protected Message(byte[] id, byte payloadD, byte ttl, byte hop,
                      int payloadL, InetSocketAddress recipient) {
        this.id = new BigInteger(id);
        this.payloadD = payloadD;
        this.ttl = ttl;
        this.hop = hop;
        this.payloadL = payloadL;
        this.recipient = recipient;

    }

    /**
     * @param idMessage    A 16-byte string uniquely identifying the descriptor on the
     *                     network
     * @param payloadD     0x00 = Ping, 0x01 = Pong, 0x40 = Push, 0x80 = Query, 0x81 =
     *                     QueryHit PayLoader Descriptor
     * @param ttl          Time to live. The number of times the descriptor will be
     *                     forwarded by Gnutella servents before it is removed from the
     *                     network
     * @param hop          The number of times the descriptor has been forwarded
     * @param payloadL     The length of the descriptor immediately following this header
     * @param recipient thread that received the message
     */
    protected Message(byte payloadD, byte ttl, byte hop, int payloadL,
                      InetSocketAddress recipient) {
        this.id = new BigInteger(IdGenerator.getIdMessage());
        this.payloadD = payloadD;
        this.ttl = ttl;
        this.hop = hop;
        this.payloadL = payloadL;
        this.recipient = recipient;

    }

    /**
     * Construct a Message read in the given DataInputStream. The message is
     * read byte by byte
     *
     * @param inStream DataInputStream in which the Message is read in bytes
     * @return Message of the Gnutella Protocol v0.4
     */
    public static Message nextMessage(DataInputStream inStream, InetSocketAddress origin) throws IOException {
        ByteArrayList message = new ByteArrayList();
        int idx = 0;


        while (idx < GnutellaConstants.HEADER_LENGTH) {

            message.add(inStream.readByte()); //blocks for input here
            idx++;

        }
            /* Declaracion de lo que almacenara el header del message */
        byte[] idMessage = new byte[GnutellaConstants.ID_LENGTH];
        byte payloadD;
        byte ttl;
        byte hop;
        byte[] payloadL = new byte[GnutellaConstants.PLL_LENGTH];
        int j = 0;

        byte stream[] = message.toArray();

        // Leemos el id
        for (int i = 0; i < GnutellaConstants.ID_LENGTH; i++) {
            idMessage[i] = stream[j++];
        }
        // Leemos el payload descriptor

        payloadD = stream[j++];

        // Leemos el ttl
        ttl = stream[j++];

        // Leemos el hop
        hop = stream[j++];

        // Leemos el payload length
        for (int i = 0; i < GnutellaConstants.PLL_LENGTH; i++) {
            payloadL[i] = stream[j++];
        }
        switch (payloadD) {
            case GnutellaConstants.PING:
                return new PingMessage(idMessage, ttl, hop, origin);

            case GnutellaConstants.PONG:
                ByteArrayList partPong = new ByteArrayList();
                while (inStream.available() > 0
                        && idx < GnutellaConstants.HEADER_LENGTH
                        + GnutellaConstants.PONG_PLL) {
                    partPong.add(inStream.readByte());
                    idx++;
                }

                stream = partPong.toArray();
                // Declaracion de donde almacenaremos los atributos del mensaje
                // pong
                byte[] port = new byte[GnutellaConstants.PORT_LENGTH];
                byte[] ip = new byte[GnutellaConstants.IP_LENGTH];
                byte[] nfilesh = new byte[GnutellaConstants.NF_LENGTH];
                byte[] nkbsh = new byte[GnutellaConstants.NK_LENGTH];
                j = 0;
                // Llenamos cada campo con lo que habia en el stream
                for (int i = 0; i < GnutellaConstants.PORT_LENGTH; i++) {
                    port[i] = stream[j++];
                }
                for (int i = 0; i < GnutellaConstants.IP_LENGTH; i++) {
                    ip[i] = stream[j++];
                }
                for (int i = 0; i < GnutellaConstants.NF_LENGTH; i++) {
                    nfilesh[i] = stream[j++];
                }

                for (int i = 0; i < GnutellaConstants.NK_LENGTH; i++) {
                    nkbsh[i] = stream[j++];
                }
                /* Convertimos el arreglo de byte del puerto a short */

                return new PongMessage(idMessage, ttl, hop, origin, port,
                        ip, nfilesh, nkbsh);
            case GnutellaConstants.QUERY:

                ByteArrayList partQuery = new ByteArrayList();

                while (inStream.available() > 0) {
                    partQuery.add(inStream.readByte());
                    idx++;
                }

                stream = partQuery.toArray();
                // Declaracion de donde almacenaremos los atributos del mensaje
                // pong
                byte[] minSpeed = new byte[GnutellaConstants.MINSPEEDL];

                int searchCriteriaL = partQuery.size()
                        - GnutellaConstants.MINSPEEDL - GnutellaConstants.EOS_L;

                byte[] searchCriteria = new byte[searchCriteriaL];

                j = 0;
                // Llenamos cada campo con lo que habia en el stream
                for (int i = 0; i < GnutellaConstants.MINSPEEDL; i++) {
                    minSpeed[i] = stream[j++];
                }
                for (int i = 0; i < searchCriteriaL; i++) {
                    searchCriteria[i] = stream[j++];
                }
                return new QueryMessage(idMessage, ttl, hop, searchCriteriaL,
                        origin, minSpeed, new String(searchCriteria));

            case GnutellaConstants.QUERY_HIT:
                ByteArrayList partQueryH = new ByteArrayList();

                while (inStream.available() > 0) {
                    byte b = inStream.readByte();
                    partQueryH.add(b);

                    idx++;
                }

                stream = partQueryH.toArray();

                // Declaracion de donde almacenaremos los atributos del mensaje
                // pong
                j = 0;
                //byte nHits = stream[j++];

                byte[] portQ = new byte[GnutellaConstants.PORT_LENGTH];
                byte[] ipQ = new byte[GnutellaConstants.IP_LENGTH];
                byte[] speedQ = new byte[4];
                //byte[][] fIQ = new byte[nHits][4];
                //byte[][] fSQ = new byte[nHits][4];

                //int payloadSize = partQueryH.size();
                    /*int resultL = payloadSize - //nHits
                            //* GnutellaConstants.QUERYHIT_PART_L
                            - GnutellaConstants.SERVER_ID_L;*/

                //byte[][] name = new byte[nHits][nameL];

                byte[] idServent = new byte[GnutellaConstants.SERVER_ID_L];

                // Llenamos cada campo con lo que habia en el stream
                for (int i = 0; i < GnutellaConstants.PORT_LENGTH; i++) {
                    portQ[i] = stream[j++];
                }
                for (int i = 0; i < GnutellaConstants.IP_LENGTH; i++) {
                    ipQ[i] = stream[j++];
                }

                for (int i = 0; i < 4; i++) {
                    speedQ[i] = stream[j++];
                }
//                    for (int k = 0; k < nHits; k++) {
//
//                        for (int i = 0; i < 4; i++) {
//                            fIQ[k][i] = stream[j++];
//                        }
//                        for (int i = 0; i < 4; i++) {
//                            fSQ[k][i] = stream[j++];
//                        }
//
//                        for (int i = 0; i < nameL; i++) {
//                            if (stream[j] == GnutellaConstants.END) {
//                                j++;
//                                break;
//                            }
//                            name[k][i] = stream[j++];
//                        }
//
//                    }
                int payload = stream.length - j - GnutellaConstants.SERVER_ID_L;


                byte[] result = new byte[payload];
                for (int i = 0; i < payload; i++) {
                    result[i] = stream[j++];
                }
                for (int i = 0; i < GnutellaConstants.SERVER_ID_L; i++) {
                    idServent[i] = stream[j++];
                }


                QueryHitMessage m = new QueryHitMessage(idMessage, ttl, hop,
                        stream.length, origin, portQ,
                        InetAddress.getByAddress(ipQ), speedQ, result,
                        idServent);

                return m;

            case GnutellaConstants.PUSH:
                return null;

            default:
                Message.logger.warn("unknown message type {} ", payloadD);

        }
        return null;

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

    /**
     * Returns the payloader descriptor of this Message
     *
     * @return the payloader descriptor in a byte representation
     */
    public byte getPayloadD() {
        return payloadD;
    }

    /**
     * Returns the ttl of this Message
     *
     * @return the ttl in a byte representation
     */
    public byte getTtl() {
        return ttl;
    }

    /**
     * Returns the hop of this Message
     *
     * @return the hop in a byte representation
     */
    public byte getHop() {
        return hop;
    }

    /**
     * Returns the payloader length of this Message
     *
     * @return the payloader length in a BigInteger representation
     */
    public int getPayloadL() {
        return payloadL;
    }

    /**
     * Returns the representation of this Message in bytes
     *
     * @return the representation in bytes
     */
    public byte[] toByteArray() {
        byte header[] = new byte[GnutellaConstants.HEADER_LENGTH];
        byte id[] = this.id.toByteArray();
        int i = 0;
        for (; i < GnutellaConstants.ID_LENGTH; i++) {
            header[i] = id[i];
        }
        header[i++] = getPayloadD();
        header[i++] = getTtl();
        header[i++] = getHop();
        byte pl[] = BigInteger.valueOf(getPayloadL()).toByteArray();

        i += GnutellaConstants.PLL_LENGTH - pl.length;
        for (int j = 0; j < pl.length; j++) {
            header[i++] = pl[j];

        }
        return header;

    }

    /**
     * Returns the id of this Message in textual presentation
     *
     * @return the id in a string format
     */
    public String idString() {
        return id.toString(36);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {

        return getClass().getSimpleName() + '|' + id.toString(36) + '|' + getPayloadD() + '|' + getTtl()
                + '|' + getHop() + '|' + getPayloadL();
    }

    public final byte[] idBytes() {
        return id.toByteArray();
    }

}
