//package nars.inter.gnutella;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.util.function.Consumer;
//
//import static nars.inter.gnutella.PeerThread.sendFile;
//
///**
// * Created by me on 7/8/16.
// */
//public class TestServer {
//
//    public static void main(String[] args) throws IOException, InterruptedException {
//
//        Peer a = new Peer(new FileServerModel("/proc"));
//        Peer b = new Peer(new FileServerModel("/tmp"));
//        a.connect(b.host(), b.port());
//
//        Thread.sleep(100);
//
//        System.out.println(a.neighbors);
//        System.out.println(b.neighbors);
//
//        a.ping();
//        Thread.sleep(100);
//
//        a.query("xyz");
//
//        //Thread.sleep(500);
//        //b.query("cpuinfo");
//
//    }
//
//    private static class FileServerModel implements ClientModel {
//
//        final String dir;
//
//        private FileServerModel(String dir) {
//            this.dir = dir;
//        }
//
//        @Override
//        public void onQueryHit(Client client, QueryHitMessage q) {
//            System.err.println("QUERY HIT: " + q);
//            //client.download(q.responder, q.getPort(), q.getFileName()[0], q.getFileSize()[0], 0);
//        }
//
//        @Override
//        public void data(Client client, String file, ByteBuffer b, int rangeByte) {
//            System.err.println("DOWNLOADED: " + b);
//        }
//
//        @Override
//        public void search(Client client, QueryMessage message, Consumer<QueryHitMessage> each) {
//            QueryHitMessage x = client.searchFiles(message, dir);
//            if (x!=null)
//                each.accept(x);
//        }
//
//        @Override
//        public byte[] data(Client client, String file, int rangePosition) {
//            try {
//                return sendFile(dir + '/' + file, rangePosition);
//            } catch (IOException e) {
//                e.printStackTrace();
//                return null;
//            }
//        }
//    }
//}
