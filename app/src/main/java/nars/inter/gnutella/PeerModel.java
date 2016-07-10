package nars.inter.gnutella;

import nars.inter.gnutella.message.QueryMessage;

import java.nio.ByteBuffer;
import java.util.function.Consumer;


public interface PeerModel {


    void data(Peer client, String file, ByteBuffer b, int rangeByte);

//    void search(Peer client, QueryMessage message, Consumer<QueryHitMessage> withResult);
//    void onQueryHit(Peer client, QueryHitMessage q);

    byte[] data(Peer client, String file, int rangePosition);
}
