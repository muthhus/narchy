package nars.control;

import com.google.common.collect.Iterables;
import jcog.data.FloatParam;
import jcog.pri.PLink;
import nars.Focus;
import nars.NAR;
import nars.concept.Concept;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class CompoundFocus implements Focus {

    public final FloatParam activationRate = new FloatParam(1f);

    final Focus[] sub;
    final Iterable<PLink<Concept>> iter;



    public CompoundFocus(List<? extends NAR> ii) {
        sub = new Focus[ii.size()];
        for (int i = 0; i < sub.length; i++)
            sub[i] = ii.get(i).focus();
        iter = sub.length > 1 ? Iterables.concat(sub) : sub[0];
    }

    @Override
    public PLink<Concept> activate(Concept term, float priToAdd) {

        float p = priToAdd * activationRate.floatValue() / sub.length;
        for (Focus c : sub) {
            c.activate(term, p);
        }

        //TODO collect an aggregate PLink
        return null;
    }


    @Override public float pri(@NotNull Termed termed) {
        float p = 0;
        int missed = 0;
        for (Focus c : sub) {
            float pp = c.pri(termed);
            if (pp==pp) {
                p += pp;
            } else {
                missed++;
            }
        }

        int num = sub.length;
        if (missed == num)
            return Float.NaN;

        return p/num; //average
    }

    @Override
    public Iterable<PLink<Concept>> concepts() {
        return iter;
    }

}
