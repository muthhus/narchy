package nars.term.compound;

import nars.IO;
import nars.Op;
import nars.term.Compound;
import nars.term.ProxyTerm;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Created by me on 4/19/17.
 */
public class ProxyCompound extends ProxyTerm<Compound> implements Compound {

    public ProxyCompound(Compound terms) {
        super(terms);
    }

    @Override
    public boolean equals(Object obj) {
        return Compound.equals(this, obj);
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

    @Override
    public int vars() {
        return super.vars();
    }
}
