package nars.web;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AtomicDouble;
import jcog.Util;
import nars.*;
import nars.op.Command;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.util.stream.Collectors.toList;


public class NARWeb extends WebServer {

    //private static final Logger logger = LoggerFactory.getLogger(nars.web.NARWeb.class);

    private final NarseseIOService io;
    //private final ActiveConceptService active;
    //private final MeshMap<Object, Object> net;

    final Set<NAR> nars = Sets.newConcurrentHashSet();

    public NARWeb(/*NAR nar, */int httpPort) {
        super(httpPort);

        addPrefixPath("/terminal", socket(io = new NarseseIOService(nars)));

        addPrefixPath("/nars", (x) -> {


            //x.startBlocking();
            x.getResponseSender().send(Util.jsonMapper.writeValueAsString(
                    Map.of(System.currentTimeMillis(),
                            nars.stream().map(this::summary).collect(toList()))
                    )
            );
            //OutputStream os = x.getOutputStream();
            //Util.jsonMapper.writeValue(os, nars);
            //os.close();
            //x.endExchange();

        });

//        net = MeshMap.get("d1", (k, v) -> {
//            System.out.println(k + " " + v);
//            try {
//                io.send(Util.toBytes(new Object[] { k , v }));
//            } catch (JsonProcessingException e) {
//                e.printStackTrace();
//            }
//        });


        //.addPrefixPath("/emotion", socket(new EvalService(nar, "emotion", 200)))
        //addPrefixPath("/active", socket(active = new ActiveConceptService(nar, 200, 48)));
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

    private Object summary(NAR y) {
        return Lists.newArrayList(
                y.self().toString(),
                y.terms.summary(),
                y.emotion.summary()
        );
    }

    public static void main(String[] args) throws Exception {

        int port = args.length < 1 ? 8080 : Integer.parseInt(args[0]);


        NAR nar =
                null;

        //nar.log();


        AtomicDouble fps = new AtomicDouble(5f);
        //AtomicReference<NARLoop> l = new AtomicReference<>(nar);

        InterNAR net = new InterNAR(nar, 8, port);

        nar.on("stop", (o, t, n) -> {
            nar.stop();
        });

        nar.on("start", (o, t, n) -> {

            float nextFPS = fps.floatValue();
            if (t.length > 0) {
                Term z = t[0];
                int x = $.intValue(z, -1); //TODO handle float's
                if (x >= 0)
                    fps.set(nextFPS = x);
            }
            start(nar, nextFPS);
        });
        nar.on("stat", (o, t, n) -> {

            Map<String, Object> stat = new TreeMap();
            //NARLoop ll = nar.loop;
            //stat.put("cpu",ll!=null ? ll.toString() : "paused");
            stat.put("mem", nar.terms.summary());
            stat.put("emo", nar.emotion.summary());
            stat.put("net", net.summary());

            Command.log(n, Util.jsonNode(stat).toString());
        });

        start(nar, fps.floatValue());

        Hear.wiki(nar);

        NARWeb w = new NARWeb(port);


    }

    public static @NotNull NARLoop start(NAR nar, float nextFPS) {
        return nar.startFPS(nextFPS);
    }
}
