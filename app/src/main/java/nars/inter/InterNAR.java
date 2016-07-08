package nars.inter;

import com.addthis.meshy.MeshyServer;
import nars.NAR;

import java.io.IOException;

/**
 * Peer interface for an InterNARS mesh
 * https://github.com/addthis/meshy/blob/master/src/test/java/com/addthis/
 */
public class InterNAR {

    final NAR nar;
    final MeshyServer server;

    public InterNAR(NAR n, int port) throws IOException {
        this.nar = n;
        this.server = new MeshyServer(port);
    }

    public void connect(String host, int port) {
        //server1.connectToPeer(server2.getUUID(), server2.getLocalAddress());
    }


}
