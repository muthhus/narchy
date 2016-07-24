package nars.web;

import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.Default;
import nars.util.data.MutableInteger;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 4/24/16.
 */
abstract public class TopConceptService<O> extends SynchWebsocketService {

    private final NAR nar;
    protected final MutableInteger maxConcepts;

    final AtomicBoolean ready = new AtomicBoolean(true);
    private List<O> lPrev;
    protected long now;

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
                this.now = nar.time();

                List<O /*ConceptSummary*/> l = $.newArrayList(n);
                final int[] i = {0};

                //SummaryStatistics s = new SummaryStatistics();

                ((Default)nar).core.concepts.forEach(n, c -> {
                    l.add( summarize(c, i[0]++) );
                    //s.addValue(c.pri());
                });

                //System.out.println(nar.time() + ": " + s.getMin() + " " + s.getMax());

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

    abstract O summarize(BLink<? extends Concept> c, int n);
}
