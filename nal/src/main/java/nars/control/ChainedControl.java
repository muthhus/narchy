package nars.control;

import com.google.common.collect.Iterators;
import jcog.bag.PLink;
import jcog.list.SynchronizedArrayList;
import nars.Control;
import nars.concept.Concept;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;

/**
 * adapter for a chain of controls
 */
public class ChainedControl extends SynchronizedArrayList<Control> implements Control {

    public ChainedControl(Control... c) {
        super(Control.class);
        this.addAll(Arrays.asList(c));
    }

    @Override
    public void activate(Concept term, float priToAdd) {
        for (int i = 0, controlSize = size(); i < controlSize; i++) {
            get(i).activate(term, priToAdd);
        }
    }

    @Override
    public float pri(@NotNull Termed termed) {
        float p = 0;
        for (int i = 0, controlSize = size(); i < controlSize; i++) {
            Control c = get(i);
            p += c.pri(termed);
        }
        return p;
    }

    @Override
    public Iterable<PLink<Concept>> conceptsActive() {
        int s = size();
        switch (s) {
            case 0:
                return Collections.emptyList();
            case 1:
                return get(0).conceptsActive(); //avoids the concatenated iterator default case
            default:
                return () -> {
                    return Iterators.concat(Iterators.transform(iterator(), c -> c.conceptsActive().iterator()));
                };
        }
    }

}
