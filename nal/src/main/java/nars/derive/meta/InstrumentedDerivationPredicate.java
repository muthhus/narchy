package nars.derive.meta;

import nars.$;
import nars.control.premise.Derivation;
import nars.term.Compound;
import nars.term.ProxyTerm;
import nars.term.compound.ProxyCompound;
import org.fusesource.jansi.Ansi;

abstract public class InstrumentedDerivationPredicate extends ProxyCompound implements PrediTerm<Derivation> {

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


    abstract protected void onEnter(PrediTerm<Derivation> p, Derivation d);

    abstract protected void onExit(PrediTerm<Derivation> p, Derivation d, boolean returnValue, Throwable thrown, long nanos);

}
