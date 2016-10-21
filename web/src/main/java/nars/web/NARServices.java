package nars.web;

import io.undertow.server.handlers.PathHandler;
import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.link.BLink;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.truth.Truth;
import spacegraph.web.Json;

import static nars.web.WebServer.socket;

/**
 * Created by me on 9/23/16.
 */
@Deprecated public class NARServices {

    public NARServices(NAR nar, PathHandler path) {

        path
                .addPrefixPath("/terminal", socket(new NarseseIOService(nar)))
                .addPrefixPath("/emotion", socket(new EvalService(nar, "emotion", 200)))
                .addPrefixPath("/active", socket(new TopConceptService<Object[]>(nar, 50, 32) {

                    @Override
                    Object[] summarize(BLink<? extends Concept> bc, int n) {
                        Concept c = bc.get();
                        return new Object[] {
                                Json.escape(c), //ID
                                b(bc.pri()), b(bc.dur()), b(bc.qua()),
                                termLinks(c, (int)Math.ceil(((float)n/maxConcepts.intValue())*(maxTermLinks-minTermLinks)+minTermLinks) ),
                                truth(c.beliefs()),
                                truth(c.goals()),
                                //TODO tasklinks, beliefs
                        };
                    }

                    private Object[] truth(BeliefTable b) {
                        Truth t = b.truth(now);
                        if (t == null) return new Object[] {} /* blank */;
                        return new Object[] { Math.round(100f* t.freq()), Math.round(100f * t.conf()) };
                    }

                    final int maxTermLinks = 5;
                    final int minTermLinks = 0;

                    private Object[] termLinks(Concept c, int num) {
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

                    private int b(float budgetValue) {
                        return Math.round(budgetValue  * 1000);
                    }
                }));


    }
}
