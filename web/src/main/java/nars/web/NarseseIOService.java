package nars.web;

import io.undertow.util.FastConcurrentDirectDeque;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import nars.NAR;
import nars.task.Task;
import nars.truth.Truth;
import nars.util.event.Active;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;

import static nars.nal.Tense.ETERNAL;

/**
 * Created by me on 4/21/16.
 */
public class NarseseIOService extends WebsocketService {


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
        CharSequence x = Json.collectionToJson(b);
        nar.runAsync(()-> {
            send(x);
        });
    }

    protected void queue(Task t) {
        //buffer.add(t.toString());
        Truth truth = t.truth();
        long occ = t.occurrence();
        buffer.add(
            new Object[] {
                String.valueOf(t.punc()),
                t.term().toString(),
                truth!=null ? t.freq() : 0,
                truth!=null ? t.conf() : 0,
                occ!=ETERNAL ? occ : ":",
                Math.round(t.pri()*1000),
                Math.round(t.dur()*1000),
                Math.round(t.qua()*1000),
                t.lastLogged()
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

        //System.out.println("onFullTextMessage: " + message);
        nar.input(message.getData());

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


}
