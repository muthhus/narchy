package nars.web;


import com.fasterxml.jackson.databind.JsonNode;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import jcog.Util;
import jcog.byt.DynByteSeq;
import nars.$;
import nars.IO;
import nars.NAR;
import nars.Task;
import nars.bag.leak.LeakOut;
import nars.nar.NARS;
import org.jetbrains.annotations.Nullable;
import spacegraph.web.WebsocketService;

import java.io.IOException;
import java.util.Set;

/**
 * Created by me on 4/21/16.
 */
public class NarseseIOService extends WebsocketService {

    private final Set<NAR> nars;

    //static final Logger logger = LoggerFactory.getLogger(NarseseIOService.class);


    private float fps = 4f;

//    final AppenderBase appender;

    public NarseseIOService(Set<NAR> nars) {
        super();

        this.nars = nars;
        //((ch.qos.logback.classic.Logger)NAR.logger)
//        $.LOG.addAppender(appender = new AppenderBase() {
//
//            @Override
//            protected void append(Object eventObject) {
//                if (((ILoggingEvent)eventObject).getLevel().isGreaterOrEqual(Level.INFO))
//                    output.accept(Command.logTask($.quote(((ILoggingEvent) eventObject).getLoggerName() + ", " + eventObject.toString())));
//
//            }
//        });
//
//        appender.start();

    }

    @Override
    protected void onConnect(WebSocketChannel socket) {
        super.onConnect(socket);
        socket.setAttribute("nar", buildDefault(socket));
    }


    @Override
    protected void onClose(WebSocketChannel socket, StreamSourceFrameChannel channel) throws IOException {
        @Nullable NAR n = nar(socket);
        if (n != null) {
            nars.remove(n);
            n.stop();
            socket.setAttribute("nar", null);
        }

        super.onClose(socket, channel);
    }

    //TODO decode binary msgpack array, like send does for outgoing
    @Override
    protected void onFullTextMessage(WebSocketChannel socket, BufferedTextMessage message) throws IOException {

        String data = message.getData();
        if (data.isEmpty())
            return;
        if (data.charAt(0) == '\0') {

            //handlecontrol message

            NAR nar = nar(socket);
            nar.stop();

            JsonNode builder = Util.jsonNode(data.substring(1));
            NAR n = build(builder);
            n.startFPS(fps);
            if (n != null) {
                socket.setAttribute("nar", n);
            } else {
                System.err.println("could not build: " + builder);
                //TODO send error msg
            }

        } else {
            NAR nar = nar(socket);
            if (nar != null) {
                Hear.hear(nar, data, "ui", 25);
            }

        }


    }

    private NAR postBuild(NAR n) {
        new LeakOut(n, 16, 1f) {
            @Override
            protected float send(Task task) {

                DynByteSeq dos = new DynByteSeq(8 + task.volume() * 6 /* estimate */);

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
        nars.add(n);
        return n;
    }

    private NAR build(JsonNode builder) {
        NAR d = new NARS().get(); //TODO interpret builder
        return postBuild(d);
    }

    private NAR buildDefault(WebSocketChannel socket) {
        NAR t = new NARS().get();
        t.setSelf($.p(socket.getLocalAddress().toString(), socket.getPeerAddress().toString()));
        t.startFPS(fps);
        return postBuild(t);
    }

    @Nullable
    private NAR nar(WebSocketChannel socket) {
        return (NAR) socket.getAttribute("nar");
    }
}
