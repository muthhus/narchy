package nars.inter.gnutella;

import java.math.BigInteger;
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
