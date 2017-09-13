package nars.derive.op;

import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/26/16.
 */
abstract public class UnifyTerm extends AbstractPred<Derivation> {

    @NotNull public final Term pattern;

    UnifyTerm(@NotNull Term id, @NotNull Term pattern) {
        super(id);
        this.pattern = pattern;
    }

}
