package nars.nal.meta;

import com.gs.collections.api.block.function.primitive.BooleanFunction;
import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.Op.ATOM;

/**
 * Created by me on 12/31/15.
 */
public interface BoolCondition extends Term, BooleanFunction<PremiseEval> {

    /** contant TRUE condition */
    BoolCondition TRUE = new AtomicBoolCondition() {

        @Override
        public boolean booleanValueOf(PremiseEval o) {
            return true;
        }

        @Override
        public String toString() {
            return "TRUE";
        }
    };



    @NotNull
    @Override
    default Op op() {
        return ATOM; //shouldnt this be a product?
    }
}
