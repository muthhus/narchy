package nars.inter.gnutella.message;

import nars.inter.gnutella.GnutellaConstants;

import java.net.InetSocketAddress;

/**
 * Class that defines a PingMessage defined in Gnutella Protocol v0.4
 *
 * @author Ismael Fernandez
 * @author Miguel Vilchis
 * @version 2.0
 */
public class PingMessage extends Message {
    /**
     * Creates a PingMessage with the specified idMessage, ttl, hop, receptor
     * node
     *
     * @param idMessage    A 16-byte string uniquely identifying the descriptor on the
     *                     network
     * @param ttl          Time to live. The number of times the descriptor will be
     *                     forwarded by Gnutella servents before it is removed from the
     *                     network
     * @param hop          The number of times the descriptor has been forwarded
     * @param receptorNode Id of the thread that received the message
     */
    public PingMessage(byte[] idMessage, byte ttl, byte hop,
                       InetSocketAddress receptorNode) {
        super(idMessage, GnutellaConstants.PING, ttl, hop,
                GnutellaConstants.PING_PLL, receptorNode);
    }


    /**
     * Creates a PingMessage with the specified ttl, hop, receptor node. The
     * idMessage is generated random.
     *
     * @param ttl          Time to live. The number of times the descriptor will be
     *                     forwarded by Gnutella servents before it is removed from the
     *                     network
     * @param hop          The number of times the descriptor has been forwarded
     * @param receptorNode Id of the thread that received the message
     */
    public PingMessage(byte ttl, byte hop, InetSocketAddress receptorNode) {
        super(GnutellaConstants.PING, ttl, hop, GnutellaConstants.PING_PLL,
                receptorNode);
    }

}
