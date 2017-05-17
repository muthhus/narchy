package nars.web;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.Util;
import nars.InterNAR;
import nars.NAR;
import nars.NARLoop;
import nars.nar.NARBuilder;
import nars.op.Command;
import nars.term.Term;
import nars.term.obj.IntTerm;
import nars.time.RealTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;
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
                NARBuilder.newMultiThreadNAR(3, new RealTime.DSHalf(false), true);

        //nar.log();


        AtomicDouble fps = new AtomicDouble(5f);
        AtomicReference<NARLoop> l = new AtomicReference<>(nar.loopFPS(fps.floatValue()));

        InterNAR net = new InterNAR(nar, 8, port);

        nar.on("stop", (o, t, n) -> {
            l.get().stop();
        });

        nar.on("start", (o, t, n) -> {

            float nextFPS = fps.floatValue();
            if (t.length>0) {
                Term z = t[0];
                if (z instanceof IntTerm) { //TODO handle float's
                    fps.set(nextFPS = (float)( ((IntTerm) z).val));
                }
            }
            start(nar, l, nextFPS);
        });
        nar.on("stat", (o, t, n) -> {
            NARLoop ll = nar.loop;

            Map<String,Object> stat = new TreeMap();
            stat.put("cpu",ll!=null ? ll.toString() : "paused");
            stat.put("mem", nar.terms.summary());
            stat.put("emo", nar.emotion.summary());
            stat.put("net", net.summary());

            Command.log( n, Util.toJSON(stat).toString());
        });



        Hear.wiki(nar);

        NARWeb w = new NARWeb(nar, port);

        start(nar, l, fps.floatValue());


    }

    public static void start(NAR nar, AtomicReference<NARLoop> l, float nextFPS) {
        l.set(nar.loopFPS(nextFPS));
    }
}
