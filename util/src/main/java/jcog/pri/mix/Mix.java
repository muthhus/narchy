package jcog.pri.mix;

import jcog.list.FasterList;
import jcog.meter.TelemetryRing;
import jcog.pri.Prioritized;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * mixes inputs from different identified sources in different amounts
 * collects statistics on the streams
 * @K source identifier
 * @P type of mixable content
 *
 * see: http://dr-lex.be/info-stuff/volumecontrols.html#about
 */
public class Mix<K, P extends Prioritized>  {


    public final Map<K, PSink> streams = new ConcurrentHashMap();
    final List<PSink> streamList = new CopyOnWriteArrayList<>();

    public TelemetryRing data;
    float[] now; //temporary buffer for storing current statistics before appending to history
    int historySize = 4;


        //TODO use a WeakValue map?

    final Consumer<P> target;
    public PSink[] streamID = new PSink[0];

    final AtomicBoolean busy = new AtomicBoolean(false);

    public Mix(Consumer<P> target) {
        this.target = target;
    }

    /** gets or creates a mix stream for the given key */
    public PSink stream(K x) {

        return streams.computeIfAbsent(x, xx -> {
            //nullify the history, need to create a new one for the new stream
            //TODO allow empty channel slots in the history buffer for stream alloc/dealloc
            data = null;
            PSink s = new PSink(xx, target);
            streamList.add(s);
            streamID = streamList.toArray(streamID);
            return s;
        });
    }


    /** captures state into the history, resetting any periodic statistics.
     * warning: a long value for time will not fit in float with full precision
     * it may need to be downsampled to a lower time unit first, for example
     * to isolate a more recent range of unixtime, or to use deciseconds etc
     * instead of nanoseconds
     * */
    public void commit(float time) {
        if (!busy.compareAndSet(false,true))
            return;

        try {

            TelemetryRing r = data;

            if (r == null) {

                int colsPerStream = 2;
                int n = streamID.length;
                List<String> cols = new FasterList(1+ n * colsPerStream);
                cols.add("t");
                for (int i = 0; i < n; i++) {
                    cols.add(streamID[i].id + " sum");
                    cols.add(streamID[i].id + " n");
                }
                this.data = r = new TelemetryRing(historySize, cols.toArray(new String[cols.size()]));
                now = new float[1 + colsPerStream * n];
            }

            int i = 0;
            now[i++] = time;
            for (PSink s : streamID) {
                now[i++] = (float)s.out.getSum();
                now[i++] = s.out.getN();
                s.commit();
            }

            r.commit(now);

        } finally {
            busy.set(false);
        }


    }

}
