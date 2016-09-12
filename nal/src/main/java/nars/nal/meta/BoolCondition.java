package nars.nal.meta;

import nars.Op;
import org.jetbrains.annotations.NotNull;

import static nars.Op.ATOM;

/**
 * Created by me on 12/31/15.
 */
public interface BoolCondition extends /*, BooleanFunction<PremiseEval>*/ ProcTerm {


    boolean booleanValueOf(PremiseEval p);

    @NotNull
    @Override
    default Op op() {
        return ATOM; //shouldnt this be a product?
    }

    static void run(@NotNull BoolCondition b, @NotNull PremiseEval m) {
        final int stack = m.now();
        b.booleanValueOf(m);
        m.revert(stack);
    }


}
