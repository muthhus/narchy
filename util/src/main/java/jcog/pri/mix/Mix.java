package jcog.pri.mix;

import jcog.pri.Priority;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * mixes inputs from different identified sources in different amounts
 * collects statistics on the streams
 * @K source identifier
 * @P type of mixable content
 *
 * see: http://dr-lex.be/info-stuff/volumecontrols.html#about
 */
public abstract class Mix<X extends Priority, Y extends Priority> implements PSinks<X,Y>, Function<X,Y> {


    public final Map<Object, PSink<X,Y>> streams = new ConcurrentHashMap();
    final List<PSink> streamList = new CopyOnWriteArrayList<>();

//    public TelemetryRing data;
//    float[] now; //temporary buffer for storing current statistics before appending to history
//    int historySize = 4;


        //TODO use a WeakValue map?

    public PSink[] streamID = new PSink[0];

    final AtomicBoolean busy = new AtomicBoolean(false);


    /** gets or creates a mix stream for the given key */
    @Override public PSink<X,Y> newStream(Object streamID, Consumer<Y> each) {

        return streams.computeIfAbsent(streamID, xx -> {
            //nullify the history, need to create a new one for the new stream
            //TODO allow empty channel slots in the history buffer for stream alloc/dealloc
            //data = null;
            PSink<X,Y> s = new PSink(xx, this, each);
            streamList.add(s);
            this.streamID = streamList.toArray(this.streamID);
            return s;
        });
    }


//    public void commit(Time t) {
//        //TODO downsample correctly
//    }

//    /** captures state into the history, resetting any periodic statistics.
//     * warning: a long value for time will not fit in float with full precision
//     * it may need to be downsampled to a lower time unit first, for example
//     * to isolate a more recent range of unixtime, or to use deciseconds etc
//     * instead of nanoseconds
//     * */
//    public void commit(float time) {
//        if (!busy.compareAndSet(false,true))
//            return;
//
//        try {
//
//            TelemetryRing r = data;
//
//            if (r == null) {
//
//                int colsPerStream = 2;
//                int n = streamID.length;
//                List<String> cols = new FasterList(1+ n * colsPerStream);
//                cols.add("t");
//                for (int i = 0; i < n; i++) {
//                    cols.add(streamID[i].id + " sum");
//                    cols.add(streamID[i].id + " n");
//                }
//                this.data = r = new TelemetryRing(historySize, cols.toArray(new String[cols.size()]));
//                now = new float[1 + colsPerStream * n];
//            }
//
//            int i = 0;
//            now[i++] = time;
//            for (PSink s : streamID) {
//                now[i++] = (float)s.out.getSum();
//                now[i++] = s.out.getN();
//                s.clear();
//            }
//
//            r.commit(now);
//
//        } finally {
//            busy.set(false);
//        }
//
//
//    }

}
