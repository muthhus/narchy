package nars.control;

import com.google.common.collect.Iterators;
import jcog.bag.PLink;
import jcog.list.SynchronizedArrayList;
import nars.Focus;
import nars.concept.Concept;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;


public class CompoundFocus implements Focus {

    final List<Focus> sub = new SynchronizedArrayList<Focus>(Focus.class);

    public CompoundFocus(Focus... c) {
        sub.addAll(Arrays.asList(c));
    }

    @Override
    public void activate(Concept term, float priToAdd) {
        for (int i = 0, controlSize = sub.size(); i < controlSize; i++) {
            sub.get(i).activate(term, priToAdd);
        }
    }



    @Override
    public void sample(int max, Predicate<? super PLink<Concept>> c) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public float pri(@NotNull Termed termed) {
        float p = 0;
        for (int i = 0, controlSize = sub.size(); i < controlSize; i++) {
            Focus c = sub.get(i);
            p += c.pri(termed);
        }
        return p;
    }

    @Override
    public Iterable<PLink<Concept>> conceptsActive() {
        int s = sub.size();
        switch (s) {
            case 0:
                return Collections.emptyList();
            case 1:
                return sub.get(0).conceptsActive(); //avoids the concatenated iterator default case
            default:
                return () -> {
                    return Iterators.concat(Iterators.transform(sub.iterator(), c -> c.conceptsActive().iterator()));
                };
        }
    }

}
