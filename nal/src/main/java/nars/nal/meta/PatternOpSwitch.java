package nars.nal.meta;

import nars.nal.meta.op.AbstractPatternOp.PatternOp;
import nars.term.atom.Atom;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created by me on 5/21/16.
 */
public final class PatternOpSwitch extends Atom /* TODO represent as some GenericCompound */ implements ProcTerm {

    public final ProcTerm[] proc = new ProcTerm[32]; //should be large enough
    public final int subterm;


    public PatternOpSwitch(int subterm, @NotNull Map<PatternOp, ProcTerm> cases) {
        super('"' + cases.toString() + '"');

        this.subterm = subterm;

        cases.forEach((c,p) -> proc[c.opOrdinal] = p);
    }

    @Override
    public void accept(@NotNull PremiseEval m) {
        ProcTerm p = proc[m.subOp(subterm)];
        if (p!=null) {
            p.accept(m);
        }
    }
}
