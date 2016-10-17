package nars.web;

import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Websocket handler that interfaces with a NAR.
 * mounted at a path on the server
 */
public class WebsocketRouter extends AbstractWebsocketService {

    /** channel -> connection map */
    protected final Map<String,Set<WebSocketChannel>> connections = new ConcurrentHashMap<>();

    public WebsocketRouter() {

    }

    @Override
    protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {

    }




    @Override
    public void onError(WebSocketChannel wsc, Void t, Throwable thrwbl) {

    }

    @Override
    public void complete(WebSocketChannel channel, Void context) {

    }

    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel socket) {

        /*if (log.isInfoEnabled())
            log.info(socket.getPeerAddress() + " connected websocket");*/

        if (connections.isEmpty()) {
            onStart();
        }

        socket.getReceiveSetter().set(this);
        socket.resumeReceives();

        onConnect(socket);
        //System.out.println(exchange.getRequestHeaders());

    }

    /**
     * called for each new connection
     */
    protected void onConnect(WebSocketChannel socket) {

    }

    protected void onDisconnect(WebSocketChannel socket) {

    }

    @Override
    protected void onClose(WebSocketChannel socket, StreamSourceFrameChannel channel) throws IOException {


        onDisconnect(socket);
        //connections.remove(socket);

//        if (connections.isEmpty()) {
//            onStop();
//        }



        /*if (log.isInfoEnabled())
            log.info(socket.getPeerAddress() + " disconnected websocket");*/
    }


    /**
     * called if one or more connected
     */
    public void onStart() {

    }

    /**
     * called when # connections becomes zero
     */
    public void onStop() {

    }


}
