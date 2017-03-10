package nars.web;

import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import jcog.data.byt.DynByteSeq;
import nars.IO;
import nars.NAR;
import nars.Task;
import nars.bag.leak.LeakOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.web.WebsocketService;

import java.io.IOException;

/**
 * Created by me on 4/21/16.
 */
public class NarseseIOService extends WebsocketService {

    static final Logger logger = LoggerFactory.getLogger(NarseseIOService.class);

    private final NAR nar;

    final LeakOut output;

    public NarseseIOService(NAR n) {
        super();
        this.nar = n;
        output = new LeakOut(n, 16, 1f) {
            @Override protected float send(Task task) {

                DynByteSeq dos = new DynByteSeq(8 + task.volume()*6 /* estimate */);

                try {
                    IO.writeTask2(dos, task);
                } catch (IOException e) {
                    e.printStackTrace();
                    return 0f;
                }

                NarseseIOService.this.send(dos.array());
                return 1f;
            }
        };
    }


    @Override
    protected void onFullTextMessage(WebSocketChannel socket, BufferedTextMessage message) throws IOException {
        Hear.hear(nar, message.getData(), "ui", 25);
    }



}
