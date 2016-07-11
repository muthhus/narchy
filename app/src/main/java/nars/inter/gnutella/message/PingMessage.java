package nars.inter.gnutella.message;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import nars.inter.gnutella.GnutellaConstants;

import java.io.DataInputStream;
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


    public PingMessage(ByteArrayDataInput in, InetSocketAddress origin) {
        super(GnutellaConstants.PING, in, origin);
    }

    @Override
    protected void inData(ByteArrayDataInput in)  {

    }

    @Override
    protected void outData(ByteArrayDataOutput out)  {

    }

    public PingMessage(byte ttl, byte hop, InetSocketAddress receptorNode) {
        super(GnutellaConstants.PING, ttl, hop,
                receptorNode);
    }

}
