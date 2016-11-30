package nars.nal.meta;

import nars.nal.meta.op.AbstractPatternOp.PatternOp;
import nars.term.atom.Atom;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Created by me on 5/21/16.
 */
public final class PatternOpSwitch extends Atom /* TODO represent as some GenericCompound */ implements BoolCondition {

    public final BoolCondition[] proc = new BoolCondition[32]; //should be large enough
    public final int subterm;


    public PatternOpSwitch(int subterm, @NotNull Map<PatternOp, BoolCondition> cases) {
        super('"' + cases.toString() + '"');

        this.subterm = subterm;

        cases.forEach((c,p) -> proc[c.opOrdinal] = p);
    }

    @Override
    public boolean run(@NotNull Derivation m, int now) {
        BoolCondition p = proc[m.subOp(subterm)];
        if (p!=null) {
            p.run(m, now);
        }
        return true;
    }
}
