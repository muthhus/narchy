package nars.concept;

import jcog.bag.Bag;
import jcog.pri.PriReference;
import nars.Task;
import nars.term.Term;
import nars.term.atom.Atom;
import org.jetbrains.annotations.NotNull;

/**
 * Used for binding functors and other situations where a Permanent Atom Concept is necessary
 */
public class PermanentAtomConcept extends AtomConcept implements PermanentConcept {

    public PermanentAtomConcept(@NotNull Atom atom, Bag<Term, PriReference<Term>> termLinks, Bag<Task, PriReference<Task>> taskLinks) {
        super(atom, termLinks, taskLinks);
    }

}
