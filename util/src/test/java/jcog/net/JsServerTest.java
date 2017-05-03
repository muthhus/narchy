package jcog.net;

import com.google.common.collect.Lists;
import jcog.Util;
import jcog.net.js.JsUDPServer;
import org.eclipse.collections.impl.factory.Maps;
import org.junit.Test;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import static junit.framework.TestCase.assertTrue;


public class JsServerTest {

    @Test
    public void testJS() throws SocketException, UnknownHostException {

        JsUDPServer<Runtime> server = new JsUDPServer<Runtime>(10000, Runtime::getRuntime);

        StringBuilder sb = new StringBuilder();
        UDP client = new UDP(10001) {
            @Override protected void in(DatagramPacket p, byte[] data) {
                sb.append( ": " + data.length + " \"" + new String(data) + "\" = bytes:" + Arrays.toString(data) );
            }
        };

        client.out("i.freeMemory()", 10000);

        server.outJSON(Maps.mutable.of("x", "y"), 10001 );
        server.outJSON(Lists.newArrayList("x", 1, 1.2), 10001 );

        Util.pause(1200);

        System.out.println(sb);
        assertTrue(sb.length() > 16);
        assertTrue(sb.toString().contains("\" = bytes:["));
    }
}
