package nars.derive.meta;

import nars.premise.Derivation;
import nars.term.Term;

/**
 * Created by me on 12/31/15.
 */
public interface BoolCondition extends /*, BooleanFunction<PremiseEval>*/ Term {

    //void accept(PremiseEval c, int now);

    boolean run(Derivation p);



//    static void run(@NotNull BoolCondition b, @NotNull PremiseEval m) {
//        final int stack = m.now();
//        b.booleanValueOf(m, stack);
//        m.revert(stack);
//    }


}