package nars.gui;

import jcog.pri.PriReference;
import nars.NAR;
import nars.concept.Concept;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

public class ConceptBagChart extends NARChart<PriReference<Concept>> {


    public ConceptBagChart(Iterable<PriReference<Concept>> b, int count, NAR nar) {
        super(b, count, nar);
    }




    @Override
    public void accept(PriReference<Concept> x, ItemVis<PriReference<Concept>> y) {
        float p = x.priSafe(0);

        float r, g, b;

        Concept c = x.get();
        if (c != null) if (c instanceof Atomic) {
            r = g = b = p * 0.5f;
        } else {
            float belief = 0.5f, goal = 0.5f;
            //float a = 0;

            long n = now;

            @Nullable Truth bt = c.beliefs().truth(n, dur, nar);
            if (bt != null) {
                belief = bt.freq();
                //a += bt.conf();
            }

            @Nullable Truth gt = c.goals().truth(n, dur, nar);
            if (gt != null) {
                goal = gt.freq();
                //a += gt.conf();
            }

            //a = Math.min(a, 1f);


            if (goal < 0.5f) {
                r = 0.05f + 0.75f * (0.5f - goal);
                g = 0;
            } else {
                g = 0.05f + 0.75f * (goal - 0.5f);
                r = 0;
            }

            b = 0.05f + 0.95f * belief;

            /*else if (c.hasQuestions() || c.hasQuests()) {
                r = 1; //yellow
                g = 1/2;
                b = 0;
            } */ /*else {
                r = g = b = 0;
            }*/
        }
        else {
            r = g = b = 0f;
        }

        y.update(p, r, g, b);

    }
}
