package nars.web;

import io.undertow.websockets.WebSocketConnectionCallback;
import nars.Global;
import nars.NAR;
import nars.bag.BLink;
import nars.concept.Concept;
import nars.nar.Default;
import nars.util.data.MutableInteger;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 4/24/16.
 */
abstract public class TopConceptService<O> extends SynchWebsocketService {

    private final NAR nar;
    private final MutableInteger maxConcepts;

    final AtomicBoolean ready = new AtomicBoolean(true);
    private List<O> lPrev = null;

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

                List<O /*ConceptSummary*/> l = Global.newArrayList(n);
                ((Default)nar).core.active.forEach(n, c -> {
                    l.add( summarize(c) );
                });

                if (lPrev!=null && lPrev.equals(l)) {
                    ready.set(true);
                    return;
                } else {

                    lPrev = l;

                    nar.runAsync(() -> {
                        send(l.toArray(new Object[l.size()]));
                        ready.set(true);
                    });
                }
            });
        }
    }

    abstract O summarize(BLink<? extends Concept> c);
}
