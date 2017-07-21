package nars.derive;

import nars.$;
import nars.Op;
import nars.control.premise.Derivation;
import nars.term.Compound;
import nars.term.ProxyCompound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.function.Function;

/**
 * Created by me on 5/21/16.
 */
public final class PatternOpSwitch extends ProxyCompound implements PrediTerm<Derivation> {

    public final EnumMap<Op,PrediTerm<Derivation>> cases;
    public final PrediTerm[] swtch;
    public final int subterm;

    PatternOpSwitch(int subterm, @NotNull EnumMap<Op,PrediTerm<Derivation>> cases) {
        super(/*$.impl*/ $.p($.the("op" + subterm), $.p(cases.entrySet().stream().map(e -> {
            return $.p($.quote(e.getKey().toString()), e.getValue());
        }).toArray(Term[]::new))));

        swtch = new PrediTerm[24]; //check this range
        cases.forEach((k,v) -> swtch[k.ordinal()] = v);
        this.subterm = subterm;
        this.cases = cases;
    }

    @Override
    public PrediTerm transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
        EnumMap<Op, PrediTerm<Derivation>> e2 = cases.clone();
        e2.replaceAll( ((k, v)-> v.transform(f) ));
        return new PatternOpSwitch(subterm, e2);
    }


    @Override
    public boolean test(@NotNull Derivation m) {
        int i = m.subOp(subterm);

        PrediTerm p =
                swtch[i];
                //cases.get(Op.values()[i]); //HACK use something better?
        if (p!=null) {
            return p.test(m);
        }
        return true; //N/A
    }
}
