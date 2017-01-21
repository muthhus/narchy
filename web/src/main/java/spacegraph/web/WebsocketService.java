package spacegraph.web;

import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Websocket handler that interfaces with a NAR.
 * mounted at a path on the server
 */
public abstract class WebsocketService extends AbstractWebsocketService {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketService.class);


    protected final List<WebSocketChannel> connections = new CopyOnWriteArrayList();



//    static {
//        ((JsonFactory)jsonizer.getCoderSpecific()).
//    }


    //.setForceSerializable(true)
    //.setForceClzInit(true)

    public WebsocketService() {

    }

    public static void send(WebSocketChannel socket, Object object, WebSocketCallback t) {
        /*if (object instanceof Object[]) {
            WebSockets.sendText(Json.arrayToJson((Object[]) object, new StringBuilder()).toString(), socket, t);
        } else */
        if (object instanceof String) {
            WebSockets.sendText((String) object, socket, t);
        } else if (object instanceof StringBuilder) {
            WebSockets.sendText(object.toString(), socket, t);
        } else if (object instanceof byte[]) {
            WebSockets.sendBinary(ByteBuffer.wrap((byte[]) object), socket, t);
        } else if (object instanceof ByteBuffer) {
            WebSockets.sendBinary((ByteBuffer) object, socket, t);
        } /*else {
            WebSockets.sendText(Json.jsonize(object), socket, t);
        }*/
    }

    /**
     * broadcast to all
     */
    public void send(Object object) {
        for (WebSocketChannel x : connections) {
            send(x, object);
        }
    }

    public void send(byte[] data) {
        send(ByteBuffer.wrap(data));
    }

    public void send(ByteBuffer data) {
        for (WebSocketChannel x : connections) {
            WebSockets.sendBinary(data, x, null);
        }
    }

    @Override
    protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
        logger.info("binar msg: {} {}", channel,  message.getData());
    }

    @Override
    public void onError(WebSocketChannel wsc, Void t, Throwable thrwbl) {
        //System.out.println("Error: " + thrwbl);
        //WebServer.logger.error("{}", thrwbl);
    }

    @Override
    public void complete(WebSocketChannel channel, Void context) {

        //log.info("Complete: " + channel);
    }

    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel socket) {


        synchronized (connections) {

            if (connections.isEmpty()) {
                onStart();
            }

            connections.add(socket);
        }

        socket.resumeReceives();

        socket.getReceiveSetter().set(this);

        onConnect(socket);

    }


    protected void onConnect(WebSocketChannel socket) {

    }

    protected void onDisconnect(WebSocketChannel socket) {

    }

    @Override
    protected void onClose(WebSocketChannel socket, StreamSourceFrameChannel channel) throws IOException {

        onDisconnect(socket);

        synchronized (connections) {
            connections.remove(socket);

            if (connections.isEmpty()) {
                onStop();
            }
        }

    }


    /**
     * called if one or more connected
     */
    public void onStart() {}

    /**
     * called when # connections becomes zero
     */
    public void onStop() { }

}
