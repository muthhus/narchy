package nars.inter.gnutella.message;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import nars.inter.gnutella.GnutellaConstants;

import java.net.InetSocketAddress;

/**
 * Class that defines a QueryMessage defined in Gnutella Protocol v0.4
 *
 * @author Ismael Fernandez
 * @author Miguel Vilchis
 * @version 2.0
 */
public class QueryMessage extends Message {

    public byte[] query;

    public QueryMessage(ByteArrayDataInput in, InetSocketAddress origin) {
        super(GnutellaConstants.QUERY, in, origin);
    }

    @Override
    protected void inData(ByteArrayDataInput in)  {
        int len = in.readUnsignedShort();
        query = new byte[len];
        in.readFully(query);
    }

    @Override
    protected void outData(ByteArrayDataOutput out) {
        out.writeShort(query.length);
        out.write(query);
    }


    public QueryMessage(byte ttl, byte hop,
                        InetSocketAddress receptorNode,
                        String query) {
        this(ttl, hop, receptorNode, query.getBytes());
    }

    public QueryMessage(byte ttl, byte hop,
                        InetSocketAddress receptorNode,
                        byte[] query) {
        super(GnutellaConstants.QUERY, ttl, hop, receptorNode);
        this.query = query;
    }


    public CharSequence queryString() {
        return new String(query);
    }
}
