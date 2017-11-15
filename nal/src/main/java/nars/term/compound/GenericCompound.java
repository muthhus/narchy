package nars.term.compound;

import jcog.Util;
import nars.IO;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.transform.Retemporalize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.DTERNAL;


public class GenericCompound implements Compound {

    /**
     * subterm vector
     */
    private final TermContainer subterms;


    /**
     * content hash
     */
    public final int hash;

    public final Op op;

    final int structureCached;

    private transient Term rooted = null;


    public GenericCompound(/*@NotNull*/ Op op, TermContainer subterms) {

        this.op = op;

        this.hash = Util.hashCombine((this.subterms = subterms).hashCode(), op.id);


        this.structureCached = Compound.super.structure();

//        //HACK
//        this.dynamic =
//                (op() == INH && subOpIs(1,ATOM) && subOpIs(0, PROD)) /* potential function */
//                        ||
//                (hasAll(EvalBits) && OR(Termlike::isDynamic)); /* possible function in subterms */
    }

    @Override
    public Term root() {
        if (rooted != null)
            return rooted;

        Term term = temporalize(Retemporalize.retemporalizeRoot);
        return rooted = term;
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
    public int hashCode() {
        return hash;
    }




    @Override
    public int dt() {
        return DTERNAL;
    }


    @Override
    public boolean isCommutative() {
        return op().commutative && subs() > 1;
    }

    @Override
    public final Op op() {
        return op;
    }



    @Override
    public String toString() {
        return IO.Printer.stringify(this).toString();
    }

    @Override
    public final boolean equals(@Nullable Object that) {
        if (this == that) return true;

        if (!(that instanceof Term) || hash != that.hashCode())
            return false;

        if (Compound.equals(this, (Term) that)) {
            if (that instanceof GenericCompound) {
                equivalent((GenericCompound) that);
            }
            return true;
        }
        return false;
    }

    /**
     * data sharing
     */
    private void equivalent(GenericCompound that) {
//        TermContainer otherSubterms = that.subterms;
//        TermContainer mySubterms = this.subterms;
//        if (mySubterms!=otherSubterms) {
//            if (System.identityHashCode(mySubterms) < System.identityHashCode(otherSubterms))
//                that.subterms = mySubterms;
//            else
//                this.subterms = otherSubterms;
//        }

        if (that.rooted != null && this.rooted!=this)
            this.rooted = that.rooted;
        if (this.rooted != null && that.rooted!=that)
            that.rooted = this.rooted;
    }


}
