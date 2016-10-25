package nars.web;

import nars.IO;
import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.Default;
import nars.term.Term;
import nars.util.data.MutableInteger;
import spacegraph.web.Json;
import spacegraph.web.PeriodicWebsocketService;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 4/24/16.
 */
public class ActiveConceptService extends PeriodicWebsocketService {

    private final NAR nar;
    protected final MutableInteger maxConcepts;

    final AtomicBoolean ready = new AtomicBoolean(true);
    //private List lPrev;
    protected long now;
    ByteBuffer current;

    public ActiveConceptService(NAR nar, int updatePeriodMS, int maxConcepts) {
        super(updatePeriodMS);
        this.nar = nar;
        this.maxConcepts = new MutableInteger(maxConcepts);
    }

    @Override
    protected void update() {

        if (ready.compareAndSet(true, false)) {
            nar.runLater(() -> {

                int n = maxConcepts.intValue();
//                this.now = nar.time();

//                List< /*ConceptSummary*/> l = $.newArrayList(n);
//                final int[] i = {0};


                //SummaryStatistics s = new SummaryStatistics();

                Bag<Concept> a = ((Default) nar).core.active;
                if (!a.isEmpty()) {

                    ByteArrayOutputStream bs = new ByteArrayOutputStream(4096);
                    DataOutput dos = new DataOutputStream(bs);

                    a.forEach(n, c -> {

                        try {
                            writeConceptSummary(dos, c);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                        //        return new Object[] {
                        //                Json.escape(c), //ID
                        //                b(bc.pri()), b(bc.dur()), b(bc.qua()),
                        //                termLinks(c, maxTermLinks),
                        //                truth(c.beliefs()),
                        //                truth(c.goals()),
                        //                //TODO tasklinks, beliefs
                        //        };
                        //s.addValue(c.pri());
                    });

                    ByteBuffer next = ByteBuffer.wrap(bs.toByteArray());
                    if (!Objects.equals(current, next)) {
                        send(next);
                        current = next;
                    }
                }

                ready.set(true);

                //System.out.println(nar.time() + ": " + s.getMin() + " " + s.getMax());

//                if (lPrev!=null && lPrev.equals(l)) {
//                    ready.set(true);
//                    return;
//                } else {
//
//                    lPrev = l;
//
//                    @NotNull Runnable t = () -> {
//                        send(l.toArray(new Object[l.size()]));
//                        ready.set(true);
//                    };
//                    nar.runLater(t);
//
////        logger.trace("runAsyncs run {}", t);
////
////        try {
////            return asyncs.submit(t);
////        } catch (RejectedExecutionException e) {
////            logger.error("runAsync error {} in {}", t, e);
////            return null;
////        }
//                }
            });
        }
    }

    private static void writeConceptSummary(DataOutput dos, BLink<? extends Concept> bc) throws IOException {

        //punctuation: ConceptSummary

        IO.writeBudget(dos, bc);
        IO.writeStringUTF(dos, bc.get().toString());

    }

//    private Object[] truth(BeliefTable b) {
//        Truth t = b.truth(now);
//        if (t == null) return new Object[] {} /* blank */;
//        return new Object[] { Math.round(100f* t.freq()), Math.round(100f * t.conf()) };
//    }

    final int maxTermLinks = 5;
    final int minTermLinks = 0;

    private static Object[] termLinks(Concept c, int num) {
        Bag<Term> b = c.termlinks();
        Object[] tl = new Object[ Math.min(num, b.size() )];
        final int[] n = {0};
        b.forEach(num, t -> {
            tl[n[0]++] = new Object[] {
                    Json.escape(t.get()), //ID
                    b(t.pri()), b(t.dur()), b(t.qua())
            };
        });
        return tl;
    }

    private static int b(float budgetValue) {
        return Math.round(budgetValue  * 1000);
    }
}
