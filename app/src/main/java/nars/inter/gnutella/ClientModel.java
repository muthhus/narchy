package nars.inter.gnutella;

import java.nio.ByteBuffer;


public interface ClientModel {

    void onQueryHit(Client client, QueryHitMessage q);

    void onDownload(Client client, String file, ByteBuffer b, int rangeByte);

    QueryHitMessage search(Client client, QueryMessage message);

    byte[] data(Client client, String file, int rangePosition);
}
