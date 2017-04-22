package nars.term.compound;

import jcog.Util;
import nars.IO;
import nars.Op;
import nars.Param;
import nars.concept.CompoundConcept;
import nars.term.Compound;
import nars.term.Termlike;
import nars.term.container.TermContainer;
import nars.term.util.InvalidTermException;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.Op.EvalBits;
import static nars.time.Tense.DTERNAL;


public class GenericCompound implements Compound {

    /**
     * subterm vector
     */
    @NotNull
    protected final TermContainer subterms;


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


    public transient boolean normalized, dynamic;


    public GenericCompound(@NotNull Op op, @NotNull TermContainer subterms) {
        this(op, Tense.DTERNAL, subterms);
    }

    public GenericCompound(@NotNull Op op, int dt, @NotNull TermContainer subterms) {

        if (Param.DEBUG && dt != DTERNAL) {

            int size = subterms.size();

            if (op.image && ((dt < 0) || (dt > size))) {
                throw new InvalidTermException(op, dt, "Invalid dt value for image " + op, subterms.toArray());
            }
            if (op != CONJ && (op.temporal && size != 2))
                throw new InvalidTermException(op, dt, "Invalid dt value for operator " + op, subterms.toArray());
        }

        this.op = op;

        this.subterms = subterms;

        this.dt = dt;

        this.hash = Util.hashCombine(subterms.hashCode(), op.ordinal(), dt);

        this.normalized = vars() + varPattern() == 0;

        this.dynamic =
                (op() == INH && subOpIs(1,ATOM) && subOpIs(0, PROD)) /* potential function */
                        ||
                (hasAll(EvalBits) && OR(Termlike::isDynamic)); /* possible function in subterms */
    }


    @Override
    public boolean isDynamic() {
        return dynamic;
    }

    @NotNull
    @Override
    public final TermContainer subterms() {
        return subterms;
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
    @Override
    public final void setNormalized() {
        this.normalized = true;
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

    @Override
    public final boolean equals(@Nullable Object that) {

        if (this == that)
            return true;
        if (hashCode()!=that.hashCode())
            return false;

        Compound cthat;
        if (that instanceof Compound) {
            cthat = (Compound) that;
        } else if (that instanceof CompoundConcept) { //Termed but not Task
            cthat = ((CompoundConcept) that).term();
            if (this == cthat)
                return true;
        } else {
            return false;
        }

        //return subterms.equals(cthat.subterms()) &&
        return
                subterms.equals(cthat.subterms())
                &&
                (op == cthat.op())
                &&
                (dt == cthat.dt());

        //subterm sharing:
//        if (as != cs) {
//            if (!as.equivalent(cs)) {
//                return false;
//            } else {
//                //share the subterms vector
//                if (cthat instanceof GenericCompound) {
//                    this.subterms = cs; //HACK cast sucks
//                }
//            }
//        }


    }

}
