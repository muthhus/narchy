package nars.term.compound;

import nars.IO;
import nars.Op;
import nars.Param;
import nars.nal.Tense;
import nars.term.Compound;
import nars.term.InvalidTermException;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.Tense.DTERNAL;


public class GenericCompound implements Compound {

    /**
     * subterm vector
     */
    @NotNull
    protected TermVector subterms;


    /**
     * numeric (term or "dt" temporal relation)
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
        this(op, Tense.DTERNAL, subterms);
    }

    public GenericCompound(@NotNull Op op, int dt, @NotNull TermContainer _subterms) {

        TermVector subterms = (TermVector) _subterms; //HACK for future support of alternate TermContainer impls

        if (Param.DEBUG && dt != DTERNAL) {
            if (!((op.isImage() && ((dt >= 0) || (dt < subterms.size()))) ||
                    (Op.isTemporal(op, dt, subterms.size()))))
                throw new InvalidTermException(op, dt, subterms.terms(), "Invalid dt value for operator " + op);
        }

        this.subterms = subterms;

        this.normalized = !subterms.hasAny(Op.VariableBits) && (subterms.varPatterns == 0); /* not included in the count */;
        this.op = op;

        this.dt = dt;

        this.hash = Util.hashCombine(subterms.hash, op.ordinal(), dt);
    }

    @NotNull
    @Override
    public final Op op() {
        return op;
    }


    @NotNull
    @Override
    public String toString() {
        return IO.Printer.stringify(this).toString();
    }


    @NotNull
    @Override
    public final TermContainer subterms() {
        return subterms;
    }

    @Override
    public final boolean equals(@Nullable Object that) {

        Compound cthat;
        if (that instanceof Compound) {

            if (this == that)
                return true;

            cthat = (Compound)that;

        } else if (that instanceof Termed)  {
            Term tthat = ((Termed) that).term();
            if (tthat instanceof Compound) {

                if (this == tthat)
                    return true;

                cthat = (Compound) tthat;

            } else {
                return false;
            }
        } else {
            return false;
        }


        if (hash != cthat.hashCode())
            return false;


        //subterm sharing:
        TermContainer cs = cthat.subterms();
        TermVector as = this.subterms;
        if (as != cs) {
            if (!as.equals(cs)) {
                return false;
            } else {
                //share the subterms vector
                this.subterms = (TermVector) cs; //HACK cast sucks
            }
        }
        //return subterms.equals(cthat.subterms()) &&
        return
                (op == cthat.op()) && (dt == cthat.dt());

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


    /**
     * do not call this manually, it will be set by VariableNormalization only
     */
    public final void setNormalized() {
        this.normalized = true;
    }


}
