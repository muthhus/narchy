package nars.inter.gnutella;

import java.nio.ByteBuffer;
import java.util.function.Consumer;


public interface ClientModel {

    void onQueryHit(Client client, QueryHitMessage q);

    void data(Client client, String file, ByteBuffer b, int rangeByte);

    void search(Client client, QueryMessage message, Consumer<QueryHitMessage> withResult);

    byte[] data(Client client, String file, int rangePosition);
}
