package nars.web;

import jcog.data.FloatParam;
import nars.InterNAR;
import nars.NAR;
import nars.NARLoop;
import nars.nar.NARBuilder;
import nars.time.RealTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by me on 1/21/17.
 */
public class NARWeb extends WebServer {

    private static final Logger logger = LoggerFactory.getLogger(nars.web.NARWeb.class);

    public NARWeb(NAR nar, int httpPort) {
        super(httpPort);

        addPrefixPath("/terminal", socket(new NarseseIOService(nar)));
        //.addPrefixPath("/emotion", socket(new EvalService(nar, "emotion", 200)))
        addPrefixPath("/active", socket(new ActiveConceptService(nar, 200, 48)));
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

        FloatParam fps = new FloatParam (5f);

        NAR nar =
                NARBuilder.newMultiThreadNAR(2, new RealTime.DSHalf(false), true);
        NARLoop l = nar.loop(fps.floatValue());

        nar.on("stop", (t, n) -> {
            l.pause();
            return null;
        });
        nar.on("start", (t, n) -> {
            l.atFPS(fps.floatValue());
            return null;
        });

        InterNAR net = new InterNAR(nar, 8, port);


        Hear.wiki(nar);

        new NARWeb(nar, port);


    }
}
