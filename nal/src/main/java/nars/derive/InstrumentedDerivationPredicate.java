package nars.derive;

import nars.$;
import nars.control.premise.Derivation;
import nars.term.Compound;
import nars.term.ProxyCompound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

abstract public class InstrumentedDerivationPredicate extends ProxyCompound implements PrediTerm<Derivation> {

    public InstrumentedDerivationPredicate(@NotNull PrediTerm<Derivation> inner) {
        super(inner instanceof Compound ? (Compound)inner : $.p(inner));
    }

    @Override public boolean test(Derivation derivation) {

        PrediTerm p;
        if (ref instanceof PrediTerm) {
            p = (PrediTerm) ref;
        } else {

            Term s0 = sub(0);
            if (s0 instanceof PrediTerm)
                p = (PrediTerm) s0;
            else {
                throw new UnsupportedOperationException();
                //((PrediTerm)(ref.sub(0)));
            }
        }

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
