package nars.derive.meta;

import nars.$;
import nars.control.premise.Derivation;
import nars.derive.meta.op.AbstractPatternOp.PatternOp;
import nars.term.compound.ProxyCompound;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created by me on 5/21/16.
 */
public final class PatternOpSwitch extends ProxyCompound implements PrediTerm<Derivation> {

    public final PrediTerm[] proc = new PrediTerm[32]; //should be large enough
    public final int subterm;


    public PatternOpSwitch(int subterm, @NotNull Map<PatternOp, PrediTerm> cases) {
        super($.func( $.the(subterm), $.quote( cases.toString() )));

        this.subterm = subterm;

        cases.forEach((c,p) -> proc[c.opOrdinal] = p);
    }

    @Override
    public boolean test(@NotNull Derivation m) {
        PrediTerm p = proc[m.subOp(subterm)];
        if (p!=null) {
            return p.test(m);
        }
        return true;
    }
}
