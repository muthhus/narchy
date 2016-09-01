package nars.web;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
import nars.NAR;
import nars.NARLoop;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.concept.table.BeliefTable;
import nars.index.CaffeineIndex;
import nars.index.Indexes;
import nars.link.BLink;
import nars.nar.Default;
import nars.nar.SingleThreadExecutioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.mental.Abbreviation;
import nars.op.mental.Anticipate;
import nars.op.mental.Inperience;
import nars.term.Term;
import nars.term.Termed;
import nars.time.RealtimeMSClock;
import nars.truth.Truth;
import nars.util.data.random.XorShift128PlusRandom;
import ognl.OgnlException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static io.undertow.Handlers.*;
import static io.undertow.UndertowOptions.*;
import static java.util.zip.Deflater.BEST_SPEED;


public class WebServer /*extends PathHandler*/ {


    public final NAR nar;

    public final Undertow server;
    public NARLoop loop;


    final static Logger logger = LoggerFactory.getLogger(WebServer.class);


    public static HttpHandler socket(WebSocketConnectionCallback w) {
        return websocket(w).addExtension(new PerMessageDeflateHandshake(false, BEST_SPEED));
    }


    @SuppressWarnings("HardcodedFileSeparator")
    public WebServer(NAR nar, float initialFPS, int httpPort) throws OgnlException {



        //TODO use ClassPathHandler and store the resources in the .jar

        File c = new File("./web/src/main/resources/");
        //File c = new File(WebServer.class.getResource("/").getPath());
        String cp = c.getAbsolutePath().replace("./", "");

        if (cp.contains("web/web")) //happens if run from web/ directory
            cp = cp.replace("web/web", "web");

        Path p = Paths.get(
                //System.getProperty("user.home")
                cp
        );//.toAbsolutePath();



        //https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java

        PathResourceManager resourcePath = new PathResourceManager(p, 0, true, true);
        server = Undertow.builder()
                .addHttpListener(httpPort, "0.0.0.0")
                .setServerOption(ENABLE_HTTP2, true)
                .setServerOption(ENABLE_SPDY, true)
                //.setIoThreads(4)
                .setHandler(
                    path()
                        .addPrefixPath("/", resource(
                                new CachingResourceManager(
                                        16384,
                                        16*1024*1024,
                                        new DirectBufferCache(100, 10, 1000),
                                        resourcePath,
                                        0 //7 * 24 * 60 * 60 * 1000
                                )
                            )
                                .setDirectoryListingEnabled(true)
                                .addWelcomeFiles("index.html")

                        )
                        .addPrefixPath("/terminal", socket(new NarseseIOService(nar)))
                        .addPrefixPath("/emotion", socket(new EvalService(nar, "emotion", 500)))
                        .addPrefixPath("/active", socket(new TopConceptService<Object[]>(nar, 800, 128) {

                            @Override
                            Object[] summarize(BLink<? extends Concept> bc, int n) {
                                Concept c = bc.get();
                                return new Object[] {
                                    escape(c), //ID
                                    b(bc.pri()), b(bc.dur()), b(bc.qua()),
                                    termLinks(c, (int)Math.ceil(((float)n/maxConcepts.intValue())*(maxTermLinks-minTermLinks)+minTermLinks) ),
                                    truth(c.beliefs()),
                                    truth(c.goals()),
                                    //TODO tasklinks, beliefs
                                };
                            }

                            private Object[] truth(BeliefTable b) {
                                Truth t = b.truth(now);
                                if (t == null) return new Object[] {} /* blank */;
                                return new Object[] { Math.round(100f* t.freq()), Math.round(100f * t.conf()) };
                            }

                            final int maxTermLinks = 5;
                            final int minTermLinks = 0;

                            private Object[] termLinks(Concept c, int num) {
                                Bag<Term> b = c.termlinks();
                                Object[] tl = new Object[ Math.min(num, b.size() )];
                                final int[] n = {0};
                                b.forEach(num, t -> {
                                    tl[n[0]++] = new Object[] {
                                       escape(t.get()), //ID
                                       b(t.pri()), b(t.dur()), b(t.qua())
                                    };
                                });
                                return tl;
                            }

                            private int b(float budgetValue) {
                                return Math.round(budgetValue  * 1000);
                            }
                        }))

                )
                .build();


        this.nar = nar;
        this.loop = nar.loop(initialFPS);

        logger.info("http/ws start: port={} staticFiles={}", httpPort, resourcePath.getBasePath());
        synchronized (server) {
            server.start();
        }


    }


    public void stop() {
        synchronized (server) {
            server.stop();
            logger.info("stop");

            loop.stop();
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

        NAR nar = newRealtimeNAR();

        new WebServer(nar, 25, httpPort);

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
    @NotNull
    public static Default newRealtimeNAR() {

        //Global.DEBUG = true;

                //new MapCacheBag(
                //new WeakValueHashMap<>()

                //GuavaCacheBag.make(1024*1024)
                /*new InfiniCacheBag(
                    InfiniPeer.tmp().getCache()
                )*/
                //)

        Random random = new XorShift128PlusRandom(1);
        int numConceptsPerCycle = 32;

        SingleThreadExecutioner exe = new SingleThreadExecutioner();
        Default nar = new Default(1024, numConceptsPerCycle, 3, 3, random,
                new CaffeineIndex(new DefaultConceptBuilder(random),10000000,false,exe),
                //new Indexes.WeakTermIndex(256*1024,random),
//                new GroupedMapIndex(
//                    new SoftValueHashMap(256*1024),
//                    new DefaultConceptBuilder(random)),
                new RealtimeMSClock(),exe
        );

        //nar.conceptActivation.setValue(1f/numConceptsPerCycle);
        nar.cyclesPerFrame.set(4);

        //nar.log();
//        nar.with(
//                Anticipate.class,
//                Inperience.class
//        );
//        nar.with(new Abbreviation(nar,"is"));

        return nar;
    }

}
