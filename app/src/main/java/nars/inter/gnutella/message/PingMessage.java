package nars.inter.gnutella.message;

import nars.inter.gnutella.GnutellaConstants;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Class that defines a PingMessage defined in Gnutella Protocol v0.4
 *
 * @author Ismael Fernandez
 * @author Miguel Vilchis
 * @version 2.0
 */
public class PingMessage extends Message {


    public PingMessage(DataInputStream in, InetSocketAddress origin) {
        super(GnutellaConstants.PING, in, origin);
    }

    @Override
    protected void inData(DataInput in) throws IOException {

    }

    @Override
    protected void outData(DataOutput out) throws IOException {

    }

    public PingMessage(byte ttl, byte hop, InetSocketAddress receptorNode) {
        super(GnutellaConstants.PING, ttl, hop,
                receptorNode);
    }

}
