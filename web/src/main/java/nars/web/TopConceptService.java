package nars.web;

import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.Default;
import nars.util.data.MutableInteger;
import org.jetbrains.annotations.NotNull;
import spacegraph.web.PeriodicWebsocketService;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 4/24/16.
 */
abstract public class TopConceptService<O> extends PeriodicWebsocketService {

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

                ((Default)nar).core.active.forEach(n, c -> {
                    l.add( summarize(c, i[0]++) );
                    //s.addValue(c.pri());
                });

                //System.out.println(nar.time() + ": " + s.getMin() + " " + s.getMax());

                if (lPrev!=null && lPrev.equals(l)) {
                    ready.set(true);
                    return;
                } else {

                    lPrev = l;

                    @NotNull Runnable t = () -> {
                        send(l.toArray(new Object[l.size()]));
                        ready.set(true);
                    };
                    nar.runLater(t);

//        logger.trace("runAsyncs run {}", t);
//
//        try {
//            return asyncs.submit(t);
//        } catch (RejectedExecutionException e) {
//            logger.error("runAsync error {} in {}", t, e);
//            return null;
//        }
                }
            });
        }
    }

    abstract O summarize(BLink<? extends Concept> c, int n);
}
