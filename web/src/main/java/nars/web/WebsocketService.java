package nars.web;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import nars.util.data.list.FasterList;
import nars.util.data.map.UnifriedMap;
import nars.util.meter.event.FloatGuage;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Websocket handler that interfaces with a NAR.
 * mounted at a path on the server
 */
public abstract class WebsocketService extends AbstractReceiveListener implements WebSocketCallback<Void>, WebSocketConnectionCallback {

    protected final Set<WebSocketChannel> connections = new LinkedHashSet();
    final static FSTConfiguration jsonizer = FSTConfiguration
            .createJsonConfiguration(false, false);
    static {
        jsonizer.setCrossPlatform(false);
        jsonizer.registerCrossPlatformClassMappingUseSimpleName(
                Object.class,
                Object[].class,
                String[].class,
                ArrayList.class,
                FasterList.class,
                HashMap.class,
                UnifriedMap.class,

                FloatGuage.class

        );
    }

//    static {
//        ((JsonFactory)jsonizer.getCoderSpecific()).
//    }


            //.setForceSerializable(true)
            //.setForceClzInit(true)
            ;

    public WebsocketService() {

    }

    @Override
    protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {

        //System.out.println(channel + " recv bin: " + message.getData());
    }

    /**
     * broadcast to all
     */
    public void send(Object object) {
        synchronized(connections) {
            for (WebSocketChannel s : connections)
                send(s, object);
        }
    }

    public void send(WebSocketChannel socket, Object object) {
        //System.out.println("send: " + object);


        WebSockets.sendText(ByteBuffer.wrap(jsonizer.asByteArray(object)), socket, this);
        //WebSockets.sendText(jsonizer.asJsonString(object), socket, this);



//            try {
//
//                ByteBuffer data = ByteBuffer.wrap(JSON.omDeep.writeValueAsBytes(object));
//
//                WebSockets.sendText(data, socket, this);
//
//
//            } catch (JsonProcessingException ex) {
//                ex.printStackTrace();
//            }
    }

//    @Override
//    public void complete(WebSocketChannel wsc, Void t) {
//        //System.out.println("Sent: " + wsc);
//    }

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

        /*if (log.isInfoEnabled())
            log.info(socket.getPeerAddress() + " connected websocket");*/


        synchronized (connections) {
            if (connections.isEmpty()) {
                onStart();
            }

            socket.getReceiveSetter().set(this);
            socket.resumeReceives();

            onConnect(socket);
            connections.add(socket);
        }

        /*Topic.all(nar.memory(), (k, v) -> {
            send(socket, k + ":" + v);
        });*/


//            textOutput = new TextOutput(nar) {
//
//
//                @Override
//                protected boolean output(final Channel channel, final Class event, final Object... args) {
//
//                    final String prefix = channel.getLinePrefix(event, args);
//                    final CharSequence s = channel.get(event, args);
//
//                    if (s != null) {
//                        return output(prefix, s);
//                    }
//
//
//                    return false;
//                }
//
//                @Override
//                protected boolean output(String prefix, CharSequence s) {
//                    send(socket, prefix + ": " + s);
//                    return true;
//                }
//            };

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

        synchronized (connections) {

            onDisconnect(socket);
            connections.remove(socket);

            if (connections.isEmpty()) {
                onStop();
            }
        }


        /*if (log.isInfoEnabled())
            log.info(socket.getPeerAddress() + " disconnected websocket");*/
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
