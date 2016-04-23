package nars.web;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import nars.NAR;
import nars.NARLoop;
import nars.nar.Default;
import ognl.OgnlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;
import static io.undertow.Handlers.websocket;


public class WebServer /*extends PathHandler*/ {


    public final NAR nar;

    public final Undertow server;
    public NARLoop loop;

    long idleFPS = 7 /* low alpha brainwaves */;

    final static Logger logger = LoggerFactory.getLogger(WebServer.class);


    public static HttpHandler socket(WebSocketConnectionCallback w) {
        return websocket(w).addExtension(new PerMessageDeflateHandshake());
    }


    @SuppressWarnings("HardcodedFileSeparator")
    public WebServer(NAR nar, int httpPort) throws OgnlException {



        //TODO use ClassPathHandler and store the resources in the .jar

        File c = new File("./web/src/main/resources/");
        //File c = new File(WebServer.class.getResource("/").getPath());
        String cp = c.getAbsolutePath().replace("./", "");
        Path p = Paths.get(
                //System.getProperty("user.home")
                cp
        );//.toAbsolutePath();
        logger.info("Serving resources: {}", p);



        //https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java

        server = Undertow.builder()
                .addHttpListener(httpPort, "localhost")
                .setIoThreads(4)
                .setHandler(
                    path()
                        .addPrefixPath("/", resource(
                                new CachingResourceManager(
                                        16384,
                                        16*1024*1024,
                                        new DirectBufferCache(100, 10, 1000),
                                        new PathResourceManager(p, 0, true, true),
                                        0 //7 * 24 * 60 * 60 * 1000
                                )
                            )
                                .setDirectoryListingEnabled(true)
                                .addWelcomeFiles("index.html")
                        )
                        .addPrefixPath("/ws", socket(new NarseseIOService(nar)))
                        .addPrefixPath("/summary", socket(new EvalService(nar, "emotion", 500)))
                )
                .build();


        this.nar = nar;
        this.loop = nar.loop(idleFPS);

        logger.info("HTTP+Websocket server starting: port={}", httpPort);
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
