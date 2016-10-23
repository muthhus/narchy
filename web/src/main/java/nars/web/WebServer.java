package nars.web;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import nars.$;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.util.Texts;
import nars.util.Util;
import nars.util.Wiki;
import ognl.Ognl;
import ognl.OgnlException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.irc.IRCAgent;
import spacegraph.irc.IRCServer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static io.undertow.Handlers.*;
import static io.undertow.UndertowOptions.ENABLE_HTTP2;
import static io.undertow.UndertowOptions.ENABLE_SPDY;
import static java.util.zip.Deflater.BEST_COMPRESSION;
import static spacegraph.irc.IRCAgent.hear;
import static spacegraph.irc.IRCAgent.newRealtimeNAR;


public class WebServer /*extends PathHandler*/ {


    public final Undertow server;
    private final PathHandler path;


    final static Logger logger = LoggerFactory.getLogger(WebServer.class);


    public static HttpHandler socket(WebSocketConnectionCallback w) {
        return websocket(w).addExtension(new PerMessageDeflateHandshake(false, BEST_COMPRESSION));
    }


    @SuppressWarnings("HardcodedFileSeparator")
    public WebServer(int httpPort) {


        this.path = path()
                .addPrefixPath("/", resource(

                        new FileResourceManager(getResourcePath().toFile(), 0))

//                        new CachingResourceManager(
//                                16384,
//                                16*1024*1024,
//                                new DirectBufferCache(100, 10, 1000),
//                                new PathResourceManager(getResourcePath(), 0, true, true),
//                                0 //7 * 24 * 60 * 60 * 1000
//                        ))
                                .setCachable((x) -> false)
                                .setDirectoryListingEnabled(true)
                                .addWelcomeFiles("index.html")
                );

        //https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java

        server = Undertow.builder()
                .addHttpListener(httpPort, "0.0.0.0")
                .setServerOption(ENABLE_HTTP2, true)
                .setServerOption(ENABLE_SPDY, true)
                .setIoThreads(1)
                .setHandler(path)
                .build();


//        path
//                .addPrefixPath("/{chan}/feed", socket(new WebsocketRouter()));


        logger.info("http start: port={}", httpPort);
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

        //new IRCServer("localhost", 6667);

        @NotNull Default nar = newRealtimeNAR(2048, 8, 2);
        //Default nar = new Default();

//        new IRCAgent(nar,
//                "experiment1", "irc.freenode.net",
//                //"#123xyz"
//                "#netention"
//                //"#nars"
//        ).start();

        nar.on("J", (terms) -> {
            //WARNING this is dangerous to allow open access
            String expr = Atom.unquote(terms[0]);
            Object r;
            try {
                r = Ognl.getValue(expr, nar);
            } catch (OgnlException e) {
                r = e;
            }
            return $.the(r.toString());
        });
        nar.on("read", (terms) -> {

            String protocol = terms[0].toString();
            String lookup = terms[1].toString();
            switch (protocol) {
                case "wiki": //wikipedia
                    String base = "simple.wikipedia.org";
                    //"en.wikipedia.org";
                    Wiki enWiki = new Wiki(base);

                    try {
                        //remove quotes
                        String page = enWiki.normalize(lookup.replace("\"", ""));
                        //System.out.println(page);

                        enWiki.setMaxLag(-1);

                        String html = enWiki.getRenderedText(page);
                        html = StringEscapeUtils.unescapeHtml4(html);
                        String strippedText = html.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ").toLowerCase();

                        //System.out.println(strippedText);

                        hear(nar, strippedText, page, 200 /*ms per word */);

                        return $.the("Reading " + base + ":" + page + ": " + strippedText.length() + " characters...");

                    } catch (IOException e) {
                        return $.the(e.toString());
                    }

                case "url":
                    break;

                //...

            }

            return $.the("Unknown protocol");

        });
        nar.on("clear", (terms) -> {
            long dt = Util.time(() -> {
                ((Default) nar).core.active.clear();
            });
            return $.the("Ready (" + dt + " ms)");
        });
        nar.on("mem", (terms) ->
                $.quote(nar.concepts.summary())
        );
        nar.on("top", (terms) -> {

                    int length = 10;
                    List<Term> b = $.newArrayList();
                    @NotNull Bag<Concept> cbag = ((Default) nar).core.active;

                    String query;
//            if (arguments.size() > 0 && arguments.term(0) instanceof Atom) {
//                query = arguments.term(0).toString().toLowerCase();
//            } else {
                    query = null;
//            }

                    cbag.topWhile(c -> {
                        String bs = c.get().toString();
                        if (query == null || bs.toLowerCase().contains(query)) {
                            b.add($.p(c.get().term(),
                                    $.quote(
                                            //TODO better summary, or use a table representation
                                            Texts.n2(c.pri())
                                    )));
                        }
                        return b.size() <= length;
                    });

                    return $.p(b);
                }

        );

        new nars.web.NARServices(nar, w.path);

        //new IRCAgent(nar, "localhost", "NARchy", "#x");





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
