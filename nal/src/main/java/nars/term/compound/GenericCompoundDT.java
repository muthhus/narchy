package nars.term.compound;

import jcog.Util;
import nars.$;
import nars.IO;
import nars.Op;
import nars.Param;
import nars.term.Compound;
import nars.term.ProxyCompound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.util.InvalidTermException;
import org.jetbrains.annotations.NotNull;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

public class GenericCompoundDT extends ProxyCompound {

    /**
     * numeric (term or "dt" temporal relation)
     */
    public final int dt;
    private final int hashDT;

    public GenericCompoundDT(GenericCompound base, int dt) {
        super(base);

        assert dt != DTERNAL : "use GenericCompound if dt==DTERNAL";

        if (Param.DEBUG) {

            Op op = base.op();

            @NotNull TermContainer subterms = base.subterms();
            int size = subterms.size();

            if (op.image) {
                if (((dt < 0) || (dt > size)) && !(dt == DTERNAL && subterms.varPattern() > 0))
                    throw new InvalidTermException(op, dt, "Invalid dt value for image", subterms.toArray());
            } /*else {
                if (subterms.containsRecursively(Op.Imdex))
                    throw new InvalidTermException(op, dt, "Illegal Imdex in Subterms", subterms.toArray());
            }*/

            if (op.temporal && (op != CONJ && size != 2))
                throw new InvalidTermException(op, dt, "Invalid dt value for operator", subterms.toArray());
        }

        this.dt = dt;
        this.hashDT = Util.hashCombine(base.hash, dt);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (obj instanceof GenericCompoundDT) {
            GenericCompoundDT d = (GenericCompoundDT)obj;
            if (hashDT!=d.hashDT) return false;
            if (dt!=d.dt()) return false;
            return ref.equals(d.ref);
        } else if (obj instanceof ProxyCompound) {
            return equals(((ProxyCompound)obj).ref);
        }

        return false;
    }

    @Override
    public boolean isCommutative() {
        if (op().commutative) { //TODO only test dt() if equiv or conj
            int dt = dt();
            switch (dt) {
                case 0:
                case DTERNAL:
                    return (size() > 1);
                case XTERNAL:
                default:
                    return false;
            }
        }
        return false;
    }

    @Override
    public boolean isTemporal() {
        return (op().temporal && (dt!=DTERNAL)) || subterms().isTemporal();
    }

    @NotNull
    @Override
    public String toString() {
        return IO.Printer.stringify(this).toString();
    }


    @Override
    public int hashCode() {
        return hashDT;
    }

    @Override
    public int dt() {
        return dt;
    }

    @Override
    public Term dt(int nextDT) {
        if (nextDT == this.dt)
            return this;

            if (op().commutative && !Op.concurrent(this.dt) && Op.concurrent(nextDT)) {
                //HACK reconstruct with sorted subterms. construct directly, bypassing ordinary TermBuilder
                @NotNull TermContainer st = subterms();
                if (!st.isSorted()) {
                    GenericCompound g = new GenericCompound(op(), TermVector.the(Terms.sorted(subterms().toArray())));
                    if (nextDT!=DTERNAL)
                        return new GenericCompoundDT( g, nextDT);
                    else
                        return g;
                }

        }
        return ref.dt(nextDT);
    }
}
