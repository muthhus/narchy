package nars.term.compound;

import nars.IO;
import nars.Op;
import nars.Param;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermVector;
import nars.term.container.TermContainer;
import nars.term.util.InvalidTermException;
import nars.time.Tense;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;


public class GenericCompound implements Compound {

    /**
     * subterm vector
     */
    @NotNull
    protected TermContainer subterms;


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

    public GenericCompound(@NotNull Op op, int dt, @NotNull TermContainer subterms) {

        if (Param.DEBUG && dt != DTERNAL) {

            if (op.image && ((dt < 0) || (dt > subterms.size()))) {
                throw new InvalidTermException(op, dt, subterms.terms(), "Invalid dt value for image " + op);
            }
            if (op != CONJ && (op.temporal && subterms.size()!=2))
                throw new InvalidTermException(op, dt, subterms.terms(), "Invalid dt value for operator " + op);
        }

        this.subterms = subterms;

        this.normalized = subterms.constant();
        this.op = op;

        this.dt = dt;

        this.hash = Util.hashCombine(subterms.hashCode(), op.ordinal(), dt);
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
    public boolean equals(@Nullable Object that) {

        Compound cthat;
        if (that instanceof Compound) {

            if (this == that)
                return true;

            cthat = (Compound)that;

        } else if (that instanceof Concept /* Termed but not Task*/)  {
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

        //subterm sharing:
        TermContainer cs = cthat.subterms();
        TermContainer as = this.subterms;
        if (as != cs) {
            if (!as.equivalent(cs)) {
                return false;
            } else {
                //share the subterms vector
                if (cthat instanceof GenericCompound) {
                    this.subterms = cs; //HACK cast sucks
                }
            }
        }

//        if (hash != cthat.hashCode())
//            return false;

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
