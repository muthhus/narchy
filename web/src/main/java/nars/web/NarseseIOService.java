package nars.web;

import io.undertow.util.FastConcurrentDirectDeque;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import nars.NAR;
import nars.Narsese;
import nars.op.in.Twenglish;
import nars.task.Task;
import nars.truth.Truth;
import nars.util.event.Active;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static nars.nal.Tense.ETERNAL;

/**
 * Created by me on 4/21/16.
 */
public class NarseseIOService extends WebsocketService {


    static final Logger logger = LoggerFactory.getLogger(NarseseIOService.class);

    private final NAR nar;
    private Active active;

    FastConcurrentDirectDeque<Object[]> buffer = new FastConcurrentDirectDeque();

    public NarseseIOService(NAR n) {
        super();
        this.nar =  n;
    }

    @Override
    public void onStart() {


        active = new Active(
                nar.eventTaskProcess.on(this::queue),
//                nar.eventAnswer.on(t -> send(
//                        "ANS: " + t)),
                nar.eventError.on(this::queue),
                nar.eventFrameStart.on(this::flush)
        );

    }

    protected void flush(NAR n) {
        if (buffer.isEmpty())
            return;

        FastConcurrentDirectDeque b = buffer;
        buffer = new FastConcurrentDirectDeque();
        CharSequence x = Json.collectionToJson(b, new StringBuilder());
        nar.runAsync(()-> send(x));
    }

    protected void queue(Task t) {
        //buffer.add(t.toString());
        Truth truth = t.truth();
        long occ = t.occurrence();
        buffer.add(
            new Object[] {
                String.valueOf(t.punc()),
                escape(t.term()),
                truth!=null ? t.freq() : 0,
                truth!=null ? t.conf() : 0,
                occ!=ETERNAL ? occ : ":",
                Math.round(t.pri()*1000),
                Math.round(t.dur()*1000),
                Math.round(t.qua()*1000),
                escape(t.lastLogged())
            }
        );
    }


    protected void queue(Object o) {
        buffer.add(new Object[] { o.toString() } );
    }

    @Override
    public void onStop() {
        active.off();
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
                if (t!=null)
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
