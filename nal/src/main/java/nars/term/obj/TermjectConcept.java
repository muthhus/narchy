package nars.term.obj;

import nars.bag.Bag;
import nars.concept.AtomConcept;
import nars.task.Task;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 7/28/16.
 */
public class TermjectConcept<X> extends AtomConcept implements Termject<X> {

    private final Termject<X> termject;

    public TermjectConcept(@NotNull Termject<X> t, Bag<Term> termLinks, Bag<Task> taskLinks) {
        super(t, termLinks, taskLinks);
        this.termject = t;
    }

    @Override
    public int complexity() {
        return term().complexity();
    }

    @Override
    public @NotNull Term term() {
        return termject;
    }

    @Override
    public X val() {
        return termject.val();
    }

    @Override
    public int compareVal(X v) {
        return termject.compareVal(v);
    }

    @Override
    public Class type() {
        return termject.type();
    }

    @Override
    public boolean match(Term y, FindSubst f) {
        return termject.match(y, f);
    }
}
