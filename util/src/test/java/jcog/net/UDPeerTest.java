package jcog.net;

import jcog.Util;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import static org.junit.Assert.assertTrue;

public class UDPeerTest {

    @Test
    public void testDiscoverableByLANMulticast() throws IOException {

        UDPeer x = new UDPeer();
        UDPeer y = new UDPeer();
        x.setFPS(4);
        y.setFPS(4);

        Util.sleep(3000);

        //discovered each other via multicast despite no explicit ping
        assertTrue(x.them.contains(y.me));
        assertTrue(y.them.contains(x.me));
    }
}
