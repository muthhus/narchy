package nars.inter.gnutella;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by me on 7/8/16.
 */
public class TestServer {
    static final ClientModel model = new ClientModel() {

        @Override
        public void onQueryHit(Client client, QueryHitMessage q) {
            System.err.println("QUERY HIT: " + q);
            client.download(q.getMyIpAddress(), q.getPort(),q.getFileName()[0], q.getFileSize()[0], 0);
        }

        @Override
        public void onDownload(Client client, String file, ByteBuffer b, int rangeByte) {
            System.err.println("DOWNLOADED: " + b);
        }

        @Override
        public QueryHitMessage search(Client client, QueryMessage message) {
            return client.searchFiles(message, "/tmp");
        }
    };

    public static void main(String[] args) throws IOException, InterruptedException {

        Peer a = new Peer("/tmp/xyz2", model);
        Peer b = new Peer("/tmp", model);
        a.connect(b.host(), b.port());

        Thread.sleep(100);

        System.out.println(a.neighbors);
        System.out.println(b.neighbors);

        a.ping();
        Thread.sleep(100);

        a.query("xyz");

        //Thread.sleep(500);
        //b.query("cpuinfo");

    }
}
