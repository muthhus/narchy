package nars.derive.op;

import nars.control.premise.Derivation;
import nars.derive.PrediTerm;
import nars.term.Compound;
import nars.term.ProxyCompound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 5/26/16.
 */
abstract public class MatchTerm extends ProxyCompound implements PrediTerm<Derivation> {

    @NotNull public final Term pattern;

    MatchTerm(@NotNull Compound id, @NotNull Term pattern) {
        super(id);
        this.pattern = pattern;
    }

}
