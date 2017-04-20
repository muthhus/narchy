package nars.term;

import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 4/19/17.
 */
public class ProxyCompound extends ProxyTerm<Compound> implements Compound  {

    public ProxyCompound(Compound terms) {
        super(terms);
    }

    @Override
    public @NotNull TermContainer subterms() {
        return ref.subterms();
    }

    @Override
    public boolean isNormalized() {
        return ref.isNormalized();
    }

    @Override
    public int dt() {
        return ref.dt();
    }

    @Override
    public void setNormalized() {
        ref.setNormalized();
    }
}
