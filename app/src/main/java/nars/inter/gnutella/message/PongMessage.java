package nars.inter.gnutella.message;

import nars.inter.gnutella.GnutellaConstants;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Class that defines a PongMessage defined in Gnutella Protocol v0.4
 *
 * @author Ismael Fernandez
 * @author Miguel Vilchis
 * @version 2.0
 */
public class PongMessage extends Message {

    private final BigInteger numberOfFileS;
    private final BigInteger numberOfKBS;
    private final BigInteger port;
    private final InetAddress ip;

    /**
     * Creates a PingMessage with the specified idMessage, ttl, hop, receptor
     *
     * @param idMessage     A 16-byte string uniquely identifying the descriptor on the
     *                      network
     * @param ttl           Time to live. The number of times the descriptor will be
     *                      forward
     * @param hop           The number of times the descriptor has been forwarded
     * @param receptorNode  Id of the thread that received the message
     * @param port          The port number on which the responding host can accept
     *                      incoming connections.
     * @param ipLE          The IP address of the responding host. The ip address must be
     *                      in Little endian. IPv4 address byte array must be 4 bytes long
     * @param numberOfFileS The number of files that the servent with the given IP address
     *                      and port is sharing on the network
     * @param numberOfKBS   The number of kilobytes of data that the servent with the
     *                      given IP address and port is sharing on the network.
     * @throws UnknownHostException if IP address is of illegal length
     */
    public PongMessage(byte[] idMessage, byte ttl, byte hop,
                       InetSocketAddress receptorNode, byte[] port, byte[] ipLE,
                       byte[] numberOfFileS, byte[] numberOfKBS)
            throws UnknownHostException {
        super(idMessage, GnutellaConstants.PONG, ttl, hop,
                GnutellaConstants.PONG_PLL, receptorNode);
        if (numberOfFileS.length > 5) {
        }
        // Mandar execepcion
        this.port = new BigInteger(port);
        this.ip = InetAddress.getByAddress(reverseArray(ipLE));
        this.numberOfFileS = new BigInteger(numberOfFileS);
        this.numberOfKBS = new BigInteger(numberOfKBS);
    }

    /**
     * Creates a PingMessage with the specified idMessage, ttl, hop, receptor
     *
     * @param idMessage     A 16-byte string uniquely identifying the descriptor on the
     *                      network
     * @param ttl           Time to live. The number of times the descriptor will be
     *                      forwarded by Gnutella servents before it is removed from the
     *                      network
     * @param hop           The number of times the descriptor has been forwarded
     * @param receptorNode  Id of the thread that received the message
     * @param port          The port number on which the responding host can accept
     *                      incoming connections.
     * @param ip            The IP address of the responding host.
     * @param numberOfFileS The number of files that the servent with the given IP address
     *                      and port is sharing on the network
     * @param numberOfKBS   The number of kilobytes of data that the servent with the
     *                      given IP address and port is sharing on the network.
     * @throws UnknownHostException if IP address is of illegal length
     */
    public PongMessage(byte[] idMessage, byte ttl, byte hop,
                       InetSocketAddress receptorNode, short port, InetAddress ip,
                       int numberOfFileS, int numberOfKBS) {
        super(idMessage, GnutellaConstants.PONG, ttl, hop,
                GnutellaConstants.PONG_PLL, receptorNode);
        this.port = BigInteger.valueOf(port);
        this.ip = ip;
        this.numberOfFileS = BigInteger.valueOf(numberOfFileS);
        this.numberOfKBS = BigInteger.valueOf(numberOfKBS);

    }

    private static byte[] reverseArray(byte[] ip) {
        int length = ip.length;
        byte ipLE[] = new byte[length];
        for (int i = 0; i < ipLE.length; i++) {
            ipLE[i] = ip[(length - 1) - i];

        }
        return ipLE;
    }

    /**
     * Return the number of files shared
     *
     * @return number of files shared
     */
    public int getNumberOfFileS() {
        return numberOfFileS.intValue();
    }

    /**
     * Return the number of Kbs shared
     *
     * @return number of Kbs shared
     */
    public int getNumberOfKBS() {
        return numberOfKBS.intValue();
    }

    /**
     * Returns the port number
     *
     * @return port number
     */
    public short getPort() {
        return port.shortValue();
    }

    /**
     * Return the ip in InetAddress format
     *
     * @return the ip
     */
    public InetAddress getIp() {
        return ip;
    }

    /**
     * Return the ip in String format
     *
     * @return the ip
     */
    public String getIpAddressString() {
        return ip.getHostName();
    }

    /*
     * (non-Javadoc)
     *
     * @see Message#toString()
     */
    public String toString() {
        return super.toString() + '|' + getPort() + '|' + getIpAddressString()
                + '|' + getNumberOfFileS() + '|' + getNumberOfKBS();
    }

    /*
     * (non-Javadoc)
     *
     * @see Message#toByteArray()
     */
    @Override
    public synchronized byte[] toByteArray() {
        byte pong[] = new byte[GnutellaConstants.HEADER_LENGTH
                + GnutellaConstants.PONG_PLL];
        byte tmpHeader[] = super.toByteArray();
        byte tmpOF[] = numberOfFileS.toByteArray();
        byte tmpKB[] = numberOfKBS.toByteArray();
        int i = 0;
        for (byte a : tmpHeader) {
            pong[i++] = a;
        }
        if (port.toByteArray().length < 2) {
            pong[i++] = 0;
        }
        for (byte a : port.toByteArray()) {
            pong[i++] = a;
        }
        byte[] ipAdress = reverseArray(ip.getAddress());
        for (byte a : ipAdress) {
            pong[i++] = a;
        }
        for (byte a : tmpOF) {
            pong[i++] = a;
        }

        for (byte a : tmpKB) {
            pong[i++] = a;
        }

        return pong;
    }
}
