package nars.inter.gnutella;

import nars.inter.gnutella.message.QueryHitMessage;
import nars.inter.gnutella.message.QueryMessage;

import java.nio.ByteBuffer;
import java.util.function.Consumer;


public interface PeerModel {

    void onQueryHit(Peer client, QueryHitMessage q);

    void data(Peer client, String file, ByteBuffer b, int rangeByte);

    void search(Peer client, QueryMessage message, Consumer<QueryHitMessage> withResult);

    byte[] data(Peer client, String file, int rangePosition);
}
