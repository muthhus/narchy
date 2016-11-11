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

    static final int termlinks = 8;

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

                    try {
                        dos.writeFloat(-1); //end of concepts
                    } catch (IOException e) { }

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

    private static void writeConceptSummary(DataOutput out, BLink<? extends Concept> bc) throws IOException {
        Concept c = bc.get();

        //punctuation: ConceptSummary

        IO.writeBudget(out, bc);

        IO.writeStringUTF(out, c.toString());

        Bag<Term> b = c.termlinks();
        b.forEach(termlinks, t -> {
            try {
                IO.writeBudget(out, t);
                IO.writeStringUTF(out, t.get().toString());
                //TODO write budget info
            } catch (IOException e) {            }
        });

        out.writeFloat(-1); //end of termlinks, will be detected when trying to read next priority
    }

//    private Object[] truth(BeliefTable b) {
//        Truth t = b.truth(now);
//        if (t == null) return new Object[] {} /* blank */;
//        return new Object[] { Math.round(100f* t.freq()), Math.round(100f * t.conf()) };
//    }

}