package nars.derive.meta;

import nars.$;
import nars.derive.meta.op.AbstractPatternOp.PatternOp;
import nars.premise.Derivation;
import nars.term.ProxyTerm;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created by me on 5/21/16.
 */
public final class PatternOpSwitch extends ProxyTerm<Term> implements BoolPred<Derivation> {

    public final BoolPred[] proc = new BoolPred[32]; //should be large enough
    public final int subterm;


    public PatternOpSwitch(int subterm, @NotNull Map<PatternOp, BoolPred> cases) {
        super($.func( $.the(subterm), $.quote( cases.toString() )));

        this.subterm = subterm;

        cases.forEach((c,p) -> proc[c.opOrdinal] = p);
    }

    @Override
    public boolean test(@NotNull Derivation m) {
        BoolPred p = proc[m.subOp(subterm)];
        if (p!=null) {
            return p.test(m);
        }
        return true;
    }
}
