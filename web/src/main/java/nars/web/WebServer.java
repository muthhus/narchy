package nars.web;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import nars.NAR;
import nars.NARLoop;
import nars.nar.Default;
import nars.util.event.Active;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;


public class WebServer extends PathHandler {


    public final NAR nar;

    public final Undertow server;
    public NARLoop loop;

    long idleFPS = 7 /* low alpha brainwaves */;

    final static Logger logger = LoggerFactory.getLogger(WebServer.class);

    public class WebSocketCore extends AbstractReceiveListener implements WebSocketCallback<Void>, WebSocketConnectionCallback {


        private Active active;

        public WebSocketCore() {
        }


        public HttpHandler get() {
            return websocket(this).addExtension(new PerMessageDeflateHandshake());
        }


        @Override
        public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel socket) {

            /*if (log.isInfoEnabled())
                log.info(socket.getPeerAddress() + " connected websocket");*/

            socket.getReceiveSetter().set(this);
            socket.resumeReceives();

            /*Topic.all(nar.memory(), (k, v) -> {
                send(socket, k + ":" + v);
            });*/


            active = new Active(
                    nar.eventInput.on(t -> send(socket,
                            " IN: " + t)),
                /*nar.memory.eventDerived.on(t -> send(socket,
                        "DER: " + t)),*/
                    nar.eventAnswer.on(t -> send(socket,
                            "ANS: " + t)),
                    nar.eventError.on(t -> send(socket,
                            "ERR: " + t))
            );

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

        @Override
        protected void onClose(WebSocketChannel socket, StreamSourceFrameChannel channel) throws IOException {

            active.off();

            /*if (log.isInfoEnabled())
                log.info(socket.getPeerAddress() + " disconnected websocket");*/
        }


        @Override
        protected void onFullTextMessage(WebSocketChannel socket, BufferedTextMessage message) throws IOException {

            //System.out.println("onFullTextMessage: " + message);
            nar.input(message.getData());

//            if (attemptJSONParseOfText) {
//                try {
//                    System.out.println(socket + " recv txt: " + message.getData());
//                    //JsonNode j = Core.json.readValue(message.getData(), JsonNode.class);
//                    //onJSONMessage(socket, j);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
        }


        @Override
        protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {

            //System.out.println(channel + " recv bin: " + message.getData());
        }


        public void send(WebSocketChannel socket, Object object) {
            //System.out.println("send: " + object);

            WebSockets.sendText(object.toString(), socket, this);

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
        }

        @Override
        public void complete(WebSocketChannel channel, Void context) {
            //log.info("Complete: " + channel);
        }
    }

    @SuppressWarnings("HardcodedFileSeparator")
    public WebServer(NAR nar, int httpPort) throws URISyntaxException {

        this.nar = nar;

        //websockets = new NARSWebSocketServer(new InetSocketAddress(webSocketsPort));
        //websockets.start();

        //TODO use resource path




        //TODO use correct resource path:
        File c = new File("./web/src/main/resources/");
        //File c = new File(WebServer.class.getResource("/").getPath());
        logger.info("Serving resources: {}", c.getAbsolutePath());


        //https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java
        addPrefixPath("/", resource(
                new FileResourceManager(c, 100)).
                setDirectoryListingEnabled(false));
        addPrefixPath("/ws", new WebSocketCore().get());

        server = Undertow.builder()
                .addHttpListener(httpPort, "localhost")
                .setIoThreads(2)
                .setHandler(this)
                .build();

        logger.info("NARS Web Server starting, port={}", httpPort);

        //narThread.start();
        //TextOutput.out(nar).setShowInput(false);

        loop = nar.loop(idleFPS);

        server.start();

    }


    public void stop() {
        synchronized (server) {
            server.stop();

            try {
                loop.stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {


//                new Default(
//                new Memory(
//                        new RealtimeMSClock(),
//                        new XorShift1024StarRandom(1),
//                        GuavaCacheBag.make(
//                            1024*1024
//                        )),
//                1024,
//                1, 2, 3
//        );

        //nar.forEachConcept(c -> System.out.println(c));

        int httpPort = args.length < 1 ? 8080 : Integer.parseInt(args[0]);

        new WebServer(new Default(1024, 1, 1, 3), httpPort);

        /*if (nlp!=null) {
            System.out.println("  NLP enabled, using: " + nlpHost + ":" + nlpPort);
        }*/

    }


    /**
     * Sends <var>text</var> to all currently connected WebSocket clients.
     *
     * @param text The String to send across the network.
     * @throws InterruptedException When socket related I/O errors occur.
     */
    /*public void sendToAll(String text) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                c.send(text);
            }
        }
    }*/


}
