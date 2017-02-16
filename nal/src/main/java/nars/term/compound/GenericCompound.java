package nars.term.compound;

import jcog.Util;
import nars.IO;
import nars.Op;
import nars.Param;
import nars.term.Compound;
import nars.term.container.TermContainer;
import nars.term.util.InvalidTermException;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;


public class GenericCompound implements Compound  {

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

    @NotNull public final Op op;


    public transient boolean normalized;


    public GenericCompound(@NotNull Op op, @NotNull TermContainer subterms) {
        this(op, Tense.DTERNAL, subterms);
    }

    public GenericCompound(@NotNull Op op, int dt, @NotNull TermContainer subterms) {

        if (Param.DEBUG && dt != DTERNAL) {

            if (op.image && ((dt < 0) || (dt > subterms.size()))) {
                throw new InvalidTermException(op, dt, "Invalid dt value for image " + op, subterms.terms());
            }
            if (op != CONJ && (op.temporal && subterms.size()!=2))
                throw new InvalidTermException(op, dt, "Invalid dt value for operator " + op, subterms.terms());
        }

//        if (dt!=XTERNAL && dt!=DTERNAL && Math.abs(dt) > 2000000)
//            System.err.println("time oob");

        this.op = op;

        this.subterms = subterms;

        this.normalized = false; //force normalization to evaluate any contained functor subterms
                //!(op==INH&&subterms.term(1) instanceof AtomicString && subterms.term(0).op()==PROD)
                ///&& subterms.constant();  /* to force functor evaluation at normalization */;

        this.dt = dt;

        this.hash = Util.hashCombine(subterms.hashCode(), op.ordinal(), dt);
    }



    @Override
    public final int hashCodeSubTerms() {
        return subterms.hashCode();
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

        if (that instanceof Compound) {

            if (hashCode() != that.hashCode())
                return false;

            Compound cthat = (Compound)that;

            //return subterms.equals(cthat.subterms()) &&
            return (op == cthat.op()) &&
                    subterms.equivalent(cthat.subterms()) &&
                   (dt == cthat.dt());


        } /*else if (that instanceof Concept )  { //Termed but not Task
            Term tthat = ((Concept) that).term();
            if (tthat instanceof Compound) {

                if (this == tthat)
                    return true;

                cthat = (Compound) tthat;

            } else {
                return false;
            }
        }  else { */
        else
            return false;


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
