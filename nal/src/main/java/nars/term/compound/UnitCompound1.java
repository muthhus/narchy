package nars.term.compound;

import jcog.Util;
import nars.IO;
import nars.Op;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.container.TermVector1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.DTERNAL;

/**
 * Compound inheriting directly from TermVector1
 * NOT READY YET
 */
public class UnitCompound1 extends TermVector1 implements Compound {

    private final Op op;
    private final int hash;

    public UnitCompound1(@NotNull Op op, @NotNull Term arg) {
        super(arg);

        if (arg.vars() > 0)
            throw new UnsupportedOperationException("variable argument not supported");

        this.op = op;
        this.hash = Util.hashCombine(super.hashCode(), op.ordinal(), DTERNAL);
    }

    @Override
    public int init(@NotNull int[] meta) {
        the.init(meta);
        meta[5] |= op.bit;
        return hash;
    }

    @Override
    public int vars() {
        return the.vars();
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public int hashCodeSubTerms() {
        return super.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object that) {
        if (this == that) return true;

        Compound t;
        if (that instanceof Concept) {
            Term tt = ((Concept) that).term();
            if (!(tt instanceof Compound))
                return false;
            t = ((Compound)tt);
        } else if (that instanceof Compound)
            t = (Compound)that;
        else
            return false;

        //TODO if this is a NEG then size and dt can be assumed
        return (op==t.op()) && (t.size()==1) && (t.dt()==DTERNAL) && (the.equals(t.term(0)));
    }

    @NotNull
    @Override
    public String toString() {
        return IO.Printer.stringify(this).toString();
    }

    @Override
    public @NotNull Op op() {
        return op;
    }

    @Override
    public @NotNull TermContainer subterms() {
        return this;
    }

    @Override
    public boolean isNormalized() {
        return the.isNormalized();
    }

    @Override
    public void setNormalized() {
        ((Compound)the).setNormalized();
    }

    @Override
    public int dt() {
        return DTERNAL;
    }

}
