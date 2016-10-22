package nars.concept;

import nars.$;
import nars.Task;
import nars.bag.Bag;
import nars.nal.nal8.operator.TermFunction;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.transform.TermTransform;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

import static nars.$.the;


abstract public class Command extends AtomConcept implements TermTransform {

    public Command(@NotNull String atom) {
        this(the(atom));
    }

    public Command(@NotNull Atomic atom) {
        this(atom, Bag.EMPTY, Bag.EMPTY);
    }

    public Command(@NotNull Atomic atom, Bag<Term> termLinks, Bag<Task> taskLinks) {
        super(atom, termLinks, taskLinks);
    }

}
