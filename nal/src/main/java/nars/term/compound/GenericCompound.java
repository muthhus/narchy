package nars.term.compound;

import jcog.Util;
import nars.IO;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.DTERNAL;


public class GenericCompound implements Compound {

    /**
     * subterm vector
     */
    @NotNull
    private TermContainer subterms;


    /**
     * content hash
     */
    public final int hash;

    @NotNull
    public final Op op;

    final int structureCached;

    public transient boolean normalized;


    public GenericCompound(/*@NotNull*/ Op op, @NotNull TermContainer subterms) {

        this.op = op;

        this.hash = Util.hashCombine(subterms.hashCode(), op.id);

        this.normalized = !(subterms.vars() > 0 || subterms.varPattern() > 0);

        this.subterms = subterms;
        this.structureCached = Compound.super.structure();

//        //HACK
//        this.dynamic =
//                (op() == INH && subOpIs(1,ATOM) && subOpIs(0, PROD)) /* potential function */
//                        ||
//                (hasAll(EvalBits) && OR(Termlike::isDynamic)); /* possible function in subterms */
    }

    @Override
    public final int structure() {
        return structureCached;
    }

    //    @Override
//    public boolean isDynamic() {
//        return dynamic;
//    }

    @NotNull
    @Override
    public final TermContainer subterms() {
        return subterms;
    }

    @Override
    public boolean contains(Term t) {
        return subterms.contains(t);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public final boolean isNormalized() {
        return normalized;
    }


    @Override
    public int dt() {
        return DTERNAL;
    }

    /**
     * do not call this manually, it will be set by VariableNormalization only
     */
    @Override
    public final void setNormalized() {
        this.normalized = true;
    }

    @Override
    public boolean isCommutative() {
        return op().commutative && subs() > 1;
    }

    @NotNull
    @Override
    public final Op op() {
        return op;
    }

    @Override
    public final int hashCodeSubTerms() {
        return subterms.hashCode();
    }

    @NotNull
    @Override
    public String toString() {
        return IO.Printer.stringify(this).toString();
    }

    @Override
    public final boolean equals(@Nullable Object that) {
        if (this==that) return true;

        if (!(that instanceof Term) || hash != that.hashCode())
            return false;

        if (Compound.equals(this, that)) {
            if (that instanceof GenericCompound) {
                equivalent((GenericCompound)that);
            }
            return true;
        }
        return false;
    }

    /** data sharing */
    private void equivalent(GenericCompound that) {
        TermContainer otherSubterms = that.subterms;
        TermContainer mySubterms = this.subterms;
        if (mySubterms!=otherSubterms) {
            if (System.identityHashCode(mySubterms) < System.identityHashCode(otherSubterms))
                that.subterms = mySubterms;
            else
                this.subterms = otherSubterms;
        }

        if (normalized ^ that.normalized) {
            //one of them is normalized so both must be
            this.normalized = that.normalized = true;
        }
    }


}
