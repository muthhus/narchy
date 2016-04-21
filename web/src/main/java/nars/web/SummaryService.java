package nars.web;

import io.undertow.websockets.WebSocketConnectionCallback;
import nars.NAR;
import nars.util.data.MutableInteger;
import nars.util.data.Util;
import nars.util.signal.Emotion;
import org.jetbrains.annotations.NotNull;
import org.nustaq.offheap.FSTAsciiStringOffheapMap;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.coders.FSTJsonEncoder;
import org.nustaq.serialization.serializers.FSTJSonSerializers;
import org.nustaq.serialization.simpleapi.DefaultCoder;
import org.nustaq.serialization.simpleapi.FSTCoder;
import org.nustaq.serialization.util.FSTUtil;

import javax.annotation.concurrent.ThreadSafe;
import java.beans.BeanInfo;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by me on 4/21/16.
 */
public class SummaryService extends WebsocketService implements Runnable {

    private final MutableInteger updatePeriodMS;
    private Thread thread;

    public SummaryService(NAR nar, int updatePeriodMS) {
        super(nar);
        this.updatePeriodMS = new MutableInteger(updatePeriodMS);
    }

    @Override
    public void onStart() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void onStop() {
        thread.interrupt();
        thread = null;
    }

    @Override
    public void run() {
        while (thread!=null) {
            update();
            Util.pause(updatePeriodMS.intValue());
        }
    }

    protected void update() {

        new NARSummary(nar) {
            @Override public void get() {
                send(data);
            }
        };
    }

    /** summarizes the state of a NAR */
    abstract public static class NARSummary implements Runnable {

        transient private final NAR nar;
        final HashMap data = new HashMap();

        public NARSummary(NAR nar) {
            this.nar = nar;

            nar.runLater(this);
        }

        @Override
        public void run() {
            put("emotion", nar.emotion );

            get();

        }

        private void put(String key, Serializable value) {
            data.put(key, value);
        }

        /** called when finished */
        abstract protected void get();
    }

}

