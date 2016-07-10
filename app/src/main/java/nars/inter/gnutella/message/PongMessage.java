package nars.inter.gnutella.message;

import nars.inter.gnutella.GnutellaConstants;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Class that defines a PongMessage defined in Gnutella Protocol v0.4
 *
 * @author Ismael Fernandez
 * @author Miguel Vilchis
 * @version 2.0
 */
public class PongMessage extends Message {


    public short port;
    public InetAddress ip;

    public PongMessage(byte[] idMessage, byte ttl, byte hop,
                       InetSocketAddress receptorNode, short port, InetAddress ip) {
        super(idMessage, GnutellaConstants.PONG, ttl, hop,
                receptorNode);
        this.port = port;
        this.ip = ip;

    }

    public PongMessage(DataInputStream in, InetSocketAddress origin) {
        super(GnutellaConstants.PONG, in, origin);
    }

    @Override
    protected void inData(DataInput in) throws IOException {
        //assumes IPv4 for now
        byte[] addr = new byte[4];
        in.readFully(addr);
        this.ip = InetAddress.getByAddress(addr);
        this.port = (short)in.readUnsignedShort();
    }

    @Override
    protected void outData(DataOutput out) throws IOException {
        out.write(ip.getAddress());
        out.writeShort(port);
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
        return super.toString() + '|' + getIpAddressString() + ':' + port;
    }

}
