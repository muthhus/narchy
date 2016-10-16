package nars.web;

import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Websocket handler that interfaces with a NAR.
 * mounted at a path on the server
 */
public abstract class WebsocketService extends AbstractWebsocketService {

    protected final Set<WebSocketChannel> connections = Collections.synchronizedSet( new HashSet<>() );


//    static {
//        ((JsonFactory)jsonizer.getCoderSpecific()).
//    }


    //.setForceSerializable(true)
    //.setForceClzInit(true)

    public WebsocketService() {

    }

    /**
     * broadcast to all
     */
    public void send(Object object) {
        for (WebSocketChannel s : connections)
            send(s, object);
    }


    @Override
    protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {

        //System.out.println(channel + " recv bin: " + message.getData());
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


        if (connections.isEmpty()) {
            onStart();
        }

        socket.getReceiveSetter().set(this);
        socket.resumeReceives();

        onConnect(socket);
        connections.add(socket);

    }


    protected void onConnect(WebSocketChannel socket) {

    }

    protected void onDisconnect(WebSocketChannel socket) {

    }

    @Override
    protected void onClose(WebSocketChannel socket, StreamSourceFrameChannel channel) throws IOException {

        onDisconnect(socket);
        connections.remove(socket);

        if (connections.isEmpty()) {
            onStop();
        }

    }


    /**
     * called if one or more connected
     */
    abstract public void onStart();

    /**
     * called when # connections becomes zero
     */
    abstract public void onStop();

}
