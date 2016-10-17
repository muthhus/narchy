package nars.web;

import com.rbruno.irc.IRCServer;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import nars.irc.IRCAgent;
import nars.nar.Default;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.undertow.Handlers.*;
import static io.undertow.UndertowOptions.ENABLE_HTTP2;
import static io.undertow.UndertowOptions.ENABLE_SPDY;
import static java.util.zip.Deflater.BEST_SPEED;
import static nars.irc.IRCAgent.newRealtimeNAR;


public class WebServer /*extends PathHandler*/ {



    public final Undertow server;
    private final PathHandler path;


    final static Logger logger = LoggerFactory.getLogger(WebServer.class);


    public static HttpHandler socket(WebSocketConnectionCallback w) {
        return websocket(w).addExtension(new PerMessageDeflateHandshake(false, BEST_SPEED));
    }


    @SuppressWarnings("HardcodedFileSeparator")
    public WebServer(int httpPort) {

        PathResourceManager resourcePath = new PathResourceManager(getResourcePath(), 0, true, true);

        this.path = path()
                .addPrefixPath("/", resource(
                        new CachingResourceManager(
                                16384,
                                16*1024*1024,
                                new DirectBufferCache(100, 10, 1000),
                                resourcePath,
                                0 //7 * 24 * 60 * 60 * 1000
                        ))
                            .setDirectoryListingEnabled(true)
                            .addWelcomeFiles("index.html")
                );

        //https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java

        server = Undertow.builder()
                .addHttpListener(httpPort, "0.0.0.0")
                .setServerOption(ENABLE_HTTP2, true)
                .setServerOption(ENABLE_SPDY, true)
                .setIoThreads(2)
                .setHandler(path)
                .build();


        path
                .addPrefixPath("/{chan}/feed", socket(new WebsocketRouter()));


        logger.info("http start: port={} staticFiles={}", httpPort, resourcePath.getBasePath());
        synchronized (server) {
            server.start();
        }


    }

    private Path getResourcePath() {
        //TODO use ClassPathHandler and store the resources in the .jar

        File c = new File("./web/src/main/resources/");
        //File c = new File(WebServer.class.getResource("/").getPath());
        String cp = c.getAbsolutePath().replace("./", "");

        if (cp.contains("web/web")) //happens if run from web/ directory
            cp = cp.replace("web/web", "web");

        return Paths.get(
                //System.getProperty("user.home")
                cp
        );
    }


    public void stop() {
        synchronized (server) {
            server.stop();
            logger.info("stop");
        }
    }

    public static void main(String[] args) throws Exception {


        int httpPort = args.length < 1 ? 8080 : Integer.parseInt(args[0]);

        WebServer w = new WebServer(httpPort);

        new IRCServer("localhost", 6667);

        @NotNull Default nar = newRealtimeNAR(1024, 2, 2);

        new NARServices(nar, w.path);

        new IRCAgent(nar, "localhost", "NARchy", "#x");



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
