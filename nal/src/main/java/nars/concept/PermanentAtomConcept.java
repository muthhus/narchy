package nars.concept;

import jcog.bag.Bag;
import jcog.pri.PLink;
import nars.Task;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;

/**
 * Used for binding functors and other situations where a Permanent Atom Concept is necessary
 */
public class PermanentAtomConcept extends AtomConcept implements PermanentConcept {
    public PermanentAtomConcept(@NotNull Atomic atom, Bag<Term, PLink<Term>> termLinks, Bag<Task, PLink<Task>> taskLinks) {
        super(atom, termLinks, taskLinks);
    }
}
