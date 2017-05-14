package nars.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import jcog.byt.DynByteSeq;
import nars.*;
import nars.bag.leak.LeakOut;
import nars.op.Command;
import spacegraph.web.WebsocketService;

import java.io.IOException;

/**
 * Created by me on 4/21/16.
 */
public class NarseseIOService extends WebsocketService  {

    //static final Logger logger = LoggerFactory.getLogger(NarseseIOService.class);

    private final NAR nar;

    final LeakOut output;

    final AppenderBase appender;

    public NarseseIOService(NAR n) {
        super();
        this.nar = n;

        //((ch.qos.logback.classic.Logger)NAR.logger)
        $.LOG.addAppender(appender = new AppenderBase() {

            @Override
            protected void append(Object eventObject) {
                if (((ILoggingEvent)eventObject).getLevel().isGreaterOrEqual(Level.INFO))
                    output.accept(Command.logTask($.quote(((ILoggingEvent) eventObject).getLoggerName() + ", " + eventObject.toString())));

            }
        });

        appender.start();

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


    //TODO decode binary msgpack array, like send does for outgoing
    @Override
    protected void onFullTextMessage(WebSocketChannel socket, BufferedTextMessage message) throws IOException {
        try {
            Hear.hear(nar, message.getData(), "ui", 25);
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        }
    }



}
