package nars.term;

import nars.IO;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Created by me on 4/19/17.
 */
public class ProxyCompound extends ProxyTerm<Compound> implements Compound  {

    public ProxyCompound(Compound terms) {
        super(terms);
    }

    @Override
    public void append(@NotNull Appendable p) throws IOException {
        IO.Printer.append(this, p);
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
