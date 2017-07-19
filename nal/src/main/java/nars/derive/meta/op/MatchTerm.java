package nars.derive.meta.op;

import nars.control.premise.Derivation;
import nars.derive.meta.PrediTerm;
import nars.term.Compound;
import nars.term.Term;
import nars.term.compound.ProxyCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 5/26/16.
 */
abstract public class MatchTerm extends ProxyCompound implements PrediTerm<Derivation> {

    @NotNull public final Compound pattern;

    public final @Nullable PrediTerm eachMatch;

    public MatchTerm(@NotNull Compound id, @NotNull Compound pattern, @Nullable PrediTerm eachMatch) {
        super(id);
        this.pattern = pattern;
        this.eachMatch = eachMatch;
    }

}
