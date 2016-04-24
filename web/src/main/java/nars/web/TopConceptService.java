package nars.web;

import io.undertow.websockets.WebSocketConnectionCallback;
import nars.Global;
import nars.NAR;
import nars.nar.Default;
import nars.util.data.MutableInteger;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 4/24/16.
 */
public class TopConceptService extends SynchWebsocketService {

    private final NAR nar;
    private final MutableInteger maxConcepts;

    final AtomicBoolean ready = new AtomicBoolean(true);
    private List<String> lPrev = null;

    public TopConceptService(NAR nar, int updatePeriodMS, int maxConcepts) {
        super(updatePeriodMS);
        this.nar = nar;
        this.maxConcepts = new MutableInteger(maxConcepts);
    }

    @Override
    protected void update() {

        if (ready.compareAndSet(true, false)) {
            nar.runLater(() -> {

                int n = maxConcepts.intValue();

                List<String /*ConceptSummary*/> l = Global.newArrayList(n);
                ((Default)nar).core.active.forEach(n, c -> {
                    l.add( c.toString() );
                });

                if (lPrev!=null && lPrev.equals(l)) {
                    ready.set(true);
                    return;
                } else {

                    lPrev = l;

                    nar.runAsync(() -> {
                        send(l.toArray(new String[l.size()]));
                        ready.set(true);
                    });
                }
            });
        }
    }
}
