package nars.web;

import com.google.common.util.concurrent.AtomicDouble;
import nars.InterNAR;
import nars.NAR;
import nars.NARLoop;
import nars.Op;
import nars.nar.NARBuilder;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.obj.IntTerm;
import nars.time.RealTime;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;


/**
 * Created by me on 1/21/17.
 */
public class NARWeb extends WebServer {

    private static final Logger logger = LoggerFactory.getLogger(nars.web.NARWeb.class);

    private final NarseseIOService io;
    private final ActiveConceptService active;

    public NARWeb(NAR nar, int httpPort) {
        super(httpPort);

        addPrefixPath("/terminal", socket(io = new NarseseIOService(nar)));
        //.addPrefixPath("/emotion", socket(new EvalService(nar, "emotion", 200)))
        addPrefixPath("/active", socket(active = new ActiveConceptService(nar, 200, 48)));
//        addPrefixPath("/json/in", socket(new WebsocketService() {
//
//            @Override
//            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
//                String s = message.getData();
//                logger.info("in: {}", s);
//                nar.believe(IO.fromJSON(s), Tense.Present, 1f);
//            }
//
//            @Override
//            public void onStart() {
//
//            }
//
//            @Override
//            public void onStop() {
//
//            }
//        }));


    }

    public static void main(String[] args) throws Exception {

        int port = args.length < 1 ? 8080 : Integer.parseInt(args[0]);


        NAR nar =
                NARBuilder.newMultiThreadNAR(2, new RealTime.DSHalf(false), true);

        AtomicDouble fps = new AtomicDouble(5f);
        AtomicReference<NARLoop> l = new AtomicReference<>(nar.loopFPS(fps.floatValue()));

        nar.on("stop", (t, n) -> {
            l.get().stop();
            return null;
        });
        nar.on("start", (t, n) -> {
            l.set(nar.loopFPS(fps.floatValue()));
            return null;
        });
        nar.on("fps", (t, n) -> {
            @Nullable Pair<Atomic, TermContainer> x = Op.functor(t.term(), nar.terms);
            if (x != null) {
                Term z = x.getTwo().sub(0);
                if (z instanceof IntTerm) { //TODO handle float's
                    l.set(nar.loopFPS( (float)( ((IntTerm) z).val)) );
                }
            }
            return null;
        });
        nar.on("cpustat", (t, n) -> {
            NARLoop ll = nar.loop;
            if (ll!=null)
                NAR.logger.info("{}", ll.toString());
            else
                NAR.logger.info("paused");

           return null;
        });

        InterNAR net = new InterNAR(nar, 8, port);


        Hear.wiki(nar);

        NARWeb w = new NARWeb(nar, port);


    }
}
