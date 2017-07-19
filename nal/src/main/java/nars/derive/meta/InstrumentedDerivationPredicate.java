package nars.derive.meta;

import nars.$;
import nars.control.premise.Derivation;
import nars.term.Compound;
import nars.term.ProxyTerm;
import nars.term.compound.ProxyCompound;

public class InstrumentedDerivationPredicate extends ProxyCompound implements PrediTerm<Derivation> {

    public InstrumentedDerivationPredicate(PrediTerm<Derivation> inner) {
        super(inner instanceof Compound ? (Compound)inner : $.p(inner));
    }

    @Override public boolean test(Derivation derivation) {
        PrediTerm p = ref instanceof PrediTerm ? (PrediTerm)ref : ((PrediTerm)(ref.sub(0)));
        onEnter(p, derivation);
        Throwable thrown = null;
        boolean result = false;
        long start = System.nanoTime();
        try {
            result = p.test(derivation);
        } catch (Throwable e) {
            thrown = e;
        }
        long end = System.nanoTime();
        onExit(p, derivation, result, thrown,end - start);
        return result;
    }


    protected void onEnter(PrediTerm<Derivation> p, Derivation d) {

    }

    protected void onExit(PrediTerm<Derivation> p, Derivation d, boolean returnValue, Throwable thrown, long nanos) {
        (thrown!=null ? System.err : System.out).println(p + " " + d + "\t" + returnValue + " " + (thrown!=null ? (thrown + " ") : "") + nanos + "nS" );
    }

}
