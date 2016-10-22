package nars.concept;

import nars.Task;
import nars.bag.Bag;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.transform.TermTransform;
import org.jetbrains.annotations.NotNull;

import static nars.$.the;


abstract public class Functor extends AtomConcept implements TermTransform, PermanentConcept {

    public Functor(@NotNull String atom) {
        this(the(atom));
    }

    public Functor(@NotNull Atomic atom) {
        this(atom, Bag.EMPTY, Bag.EMPTY);
    }

    public Functor(@NotNull Atomic atom, Bag<Term> termLinks, Bag<Task> taskLinks) {
        super(atom, termLinks, taskLinks);
    }

}
