package nars.term.compound;

import nars.Op;
import nars.nal.Tense;
import nars.term.*;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.util.data.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.Tense.DTERNAL;


public class GenericCompound<T extends Term> implements Compound<T> {

    /**
     * subterm vector
     */
    @NotNull
    public final TermVector subterms;

    /**
     * subterm relation, resolves to unique concept
     */
    public final int relation;

    /**
     * temporal relation (dt), resolves to same concept
     */
    public final int dt;

    /**
     * content hash
     */
    public final int hash;

    @NotNull
    public final Op op;

    public transient boolean normalized;

    public GenericCompound(@NotNull Op op, @NotNull TermContainer subterms) {
        this(op, -1, subterms);
    }

    public GenericCompound(@NotNull Op op, int relation, @NotNull TermContainer subterms) {
        this(op, relation, Tense.DTERNAL, (TermVector) subterms);
    }

    public GenericCompound(@NotNull Op op, int relation, int dt, @NotNull TermVector subterms) {

        if (dt!=DTERNAL && !Op.isTemporal(op, dt, subterms.size()))
            throw new InvalidTerm(op, relation, dt, subterms.terms());

        this.subterms = subterms;

        this.normalized = (subterms.vars == 0) && (subterms.varPatterns == 0) /* not included in the count */;
        this.op = op;

        this.relation = relation;

        this.dt = dt;

        this.hash = Util.hashCombine(subterms.hash, Terms.opRel(op, relation), dt);
    }

    @NotNull
    @Override
    public final Op op() {
        return op;
    }


    @NotNull
    @Override
    public String toString() {
        return TermPrinter.stringify(this).toString();
    }


    @NotNull
    @Override
    public final TermContainer subterms() {
        return subterms;
    }

    @Override
    public final boolean equals(@Nullable Object that) {

        if (this == that) return true;
        if (that!=null && hashCode() == that.hashCode()) {
            if (that instanceof Compound) {
                Compound cthat = (Compound) that;
                return (
                    opRel() == cthat.opRel() &&
                    dt() == cthat.dt() &&
                    subterms().equals(cthat.subterms())
                );
            }
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final boolean isNormalized() {
        return normalized;
    }

    @Override
    public final int dt() {
        return dt;
    }


    @Override
    public final int relation() {
        return relation;
    }

    /**
     * do not call this manually, it will be set by VariableNormalization only
     */
    public final void setNormalized() {
        this.normalized = true;
    }


}
