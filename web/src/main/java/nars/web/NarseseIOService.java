package nars.web;

import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import jcog.event.Ons;
import nars.IO;
import nars.NAR;
import nars.Task;
import nars.bag.Bag;
import nars.bag.CurveBag;
import nars.budget.BudgetMerge;
import nars.link.BLink;
import nars.nlp.Twenglish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.web.WebsocketService;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 4/21/16.
 */
public class NarseseIOService extends WebsocketService {


    static final Logger logger = LoggerFactory.getLogger(NarseseIOService.class);

    public static final int OUTPUT_CAPACITY = 128;
    public static final int OUTPUT_RATE = 4;

    private final NAR nar;
    private Ons ons;

    final Bag<Task> output;
    final AtomicBoolean queued = new AtomicBoolean(false);

    //FastConcurrentDirectDeque<byte[]> outgoing = new FastConcurrentDirectDeque();

    public NarseseIOService(NAR n) {
        super();
        this.nar = n;
        output = new CurveBag<Task>(OUTPUT_CAPACITY, new CurveBag.NormalizedSampler(CurveBag.power2BagCurve, n.random),  BudgetMerge.plusBlend, new HashMap());
        output.setCapacity(OUTPUT_CAPACITY);
    }

    @Override
    public void onStart() {

        ons = new Ons(
            nar.eventTaskProcess.on(this::output)
//                nar.eventAnswer.on(t -> send(
//                        "ANS: " + t)),
                //nar.eventError.on(this::queue),
//                nar.eventFrameStart.on(this::flush)
        );

    }

    protected void flush() {
        BLink<Task> tl;

        output.commit();

        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream(4096);
            DataOutputStream dos = new DataOutputStream(bs);

            int remaining = OUTPUT_RATE;
            while (remaining-- > 0 && (tl = output.pop()) != null) {
                IO.writeTask2(dos, tl.get());
            }

            send(bs.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
        }

        queued.set(false);

////        FastConcurrentDirectDeque b = outgoing;
////        outgoing = new FastConcurrentDirectDeque();
//        //CharSequence x = Json.collectionToJson(b, new StringBuilder(2048));
//        //nar.runLater(()-> send(x));
    }

    protected void output(Task t) {

        output.put(t);

        if (queued.compareAndSet(false, true))
            nar.runLater(this::flush);

    }


//    protected void queue(Object o) {
//        buffer.add(new Object[] { o.toString() } );
//    }

    @Override
    public void onStop() {
        ons.off();
    }

    @Override
    protected void onFullTextMessage(WebSocketChannel socket, BufferedTextMessage message) throws IOException {

        String msg = message.getData();

        boolean narsese = tryNarsese(msg);
        if (narsese)
            return;

        boolean twenglish = tryTwenglish(msg);
        if (twenglish)
            return;

        logger.error("Unrecognizable input: {}", msg);

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

    private boolean tryTwenglish(String msg) {

        try {
            new Twenglish().parse("ui", nar, msg).forEach(t -> {
                //t.setPriority(INPUT_SENTENCE_PRIORITY);
                if (t != null)
                    nar.input(t);
            });
            return true;
        } catch (Exception f) {
            logger.error("{}", f);
            return false;
        }
    }

    private boolean tryNarsese(String msg) {
        try {
            List<Task> t = nar.tasks(msg);
            if (t.isEmpty())
                return false;

            nar.input(t);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


}
