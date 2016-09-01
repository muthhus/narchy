package nars.term.obj;

import nars.Op;
import nars.term.Term;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

import static nars.Op.INT;

/**
 * Created by me on 8/29/16.
 */
public class IntTerm extends Termject.PrimTermject<Integer> {

    public IntTerm(@NotNull int val) {
        super(val);
    }

    @Override
    public @NotNull Op op() {
        return INT;
    }

    @Override
    public int compareVal(Integer v) {
        return Integer.compare(val(), v);
    }

    @NotNull
    @Override
    public Class type() {
        return Integer.class;
    }

    @Override
    public boolean unify(@NotNull Term y, @NotNull FindSubst f) {

        if (y instanceof IntInterval) {
            return y.unify(this, f); //reverse x,y necessary?
        }
        return false;
    }
}
