package nars.nal.meta;

import com.gs.collections.api.block.function.primitive.BooleanFunction;
import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.Op.ATOM;

/**
 * Created by me on 12/31/15.
 */
public interface BoolCondition extends Term/*, BooleanFunction<PremiseEval>*/, ProcTerm {


    boolean booleanValueOf(PremiseEval var1);

    @NotNull
    @Override
    default Op op() {
        return ATOM; //shouldnt this be a product?
    }

    static void run(BoolCondition b, PremiseEval m) {
        final int stack = m.now();
        b.booleanValueOf(m);
        m.revert(stack);
    }


}
