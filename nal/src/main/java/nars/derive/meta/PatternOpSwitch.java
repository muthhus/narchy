package nars.derive.meta;

import jcog.Util;
import nars.$;
import nars.control.premise.Derivation;
import nars.derive.meta.op.AbstractPatternOp.PatternOp;
import nars.term.Compound;
import nars.term.ProxyCompound;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

/**
 * Created by me on 5/21/16.
 */
public final class PatternOpSwitch extends ProxyCompound implements PrediTerm<Derivation> {

    public final PrediTerm[] cache;
    public final int subterm;


    public PatternOpSwitch(int subterm, @NotNull Map<PatternOp, PrediTerm> cases) {
        super($.func( $.the(subterm), $.quote( cases.toString() )));

        this.subterm = subterm;

        cache = new PrediTerm[32]; //should be large enough
        cases.forEach((c,p) -> cache[c.opOrdinal] = p);
    }

    protected PatternOpSwitch(Compound id, int subterm, PrediTerm[] cache) {
        super(id);
        this.subterm = subterm;
        this.cache = cache;
    }

    @Override
    public PrediTerm transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
        return new PatternOpSwitch(ref, subterm, Util.map(x -> x.transform(f), new PrediTerm[cache.length], cache));
    }


    @Override
    public boolean test(@NotNull Derivation m) {
        PrediTerm p = cache[m.subOp(subterm)];
        if (p!=null) {
            return p.test(m);
        }
        return true;
    }
}
