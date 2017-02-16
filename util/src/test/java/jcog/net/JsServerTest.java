package jcog.net;

import jcog.Util;
import org.junit.Test;

import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;

import static junit.framework.TestCase.assertTrue;


public class JsServerTest {

    @Test
    public void testJS() throws SocketException {

        JsUDPServer<Runtime> server = new JsUDPServer<Runtime>(10000, Runtime::getRuntime);

        StringBuilder sb = new StringBuilder();
        UDP client = new UDP(10001) {
            @Override protected void in(byte[] data, SocketAddress from) {
                sb.append(from + ": " + data.length + " \"" + new String(data) + "\" = bytes:" + Arrays.toString(data) );
            }
        };

        client.out("i.freeMemory()", 10000);

        Util.pause(1200);

        System.out.println(sb);
        assertTrue(sb.length() > 16);
        assertTrue(sb.toString().contains("\" = bytes:["));
    }
}
