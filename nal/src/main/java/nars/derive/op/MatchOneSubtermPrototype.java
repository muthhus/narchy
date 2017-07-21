package nars.derive.op;

import nars.$;
import nars.control.premise.Derivation;
import nars.derive.PrediTerm;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Created by me on 5/21/16.
 */
public final class MatchOneSubtermPrototype extends UnificationPrototype {

    /**
     * either 0 (task) or 1 (belief)
     */
    private final int subterm;

    private final boolean finish;

    public MatchOneSubtermPrototype(@NotNull Term x, int subterm, boolean finish) {
        super( $.func("unifyTask", $.the(subterm==0 ? "task" : "belief"), x), x );
        this.subterm = subterm;
        this.finish = finish;
    }

    @Override
    public final PrediTerm<Derivation> transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
      PrediTerm<Derivation> eachMatch = buildEachMatch();
      return build( eachMatch!=null ? eachMatch.transform(f) : null ).transform(f);
    }


    @Override @NotNull
    protected PrediTerm build(@Nullable PrediTerm eachMatch) {
        assert(!(!finish && eachMatch!=null || finish && eachMatch == null)): "conclusion wrong";
        return new MatchOneSubterm( subterm, pattern, finish ? eachMatch : null);
    }



}
