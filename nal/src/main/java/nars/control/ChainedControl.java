package nars.control;

import com.google.common.collect.Iterators;
import jcog.list.ConcurrentArrayList;
import nars.Control;
import nars.concept.Concept;
import nars.link.BLink;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * adapter for a chain of controls
 */
public class ChainedControl extends ConcurrentArrayList<Control> implements Control {

    public ChainedControl(Control... c) {
        super(Control.class);
        for (Control x : c)
            add(x);
    }

    @Override
    public void activate(Termed term, float priToAdd) {
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
    public Iterable<BLink<Concept>> conceptsActive() {
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
