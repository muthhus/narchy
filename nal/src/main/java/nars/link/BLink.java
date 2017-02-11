package nars.link;

import nars.budget.Budget;
import nars.budget.BudgetMerge;

/**
 * Created by me on 5/29/16.
 */
public interface BLink<X> extends PLink<X>, Budget {

    BLink<X> cloneScaled(BudgetMerge merge, float scale);

    /** set priority 0, quality=this.quality */
    BLink<X> cloneZero(float q);



    //    @Override public final boolean equals(Object obj) {
////        /*if (obj instanceof Budget)*/ {
////            return equalsBudget((Budget) obj);
////        }
////        return id.equals(((BagBudget)obj).id);
//        boolean result;
//        @Nullable X x = get();
//        if (obj instanceof BLink) {
//            Object o = ((BLink) obj).get();
//            result = Objects.equal(x, o);
//        } else {
//            result = Objects.equal(x, obj);
//        }
//        return obj == this || result;
//    }
//
//
//    @Override public final int hashCode() {
//        Object x = get();
//        return x == null ? 0 : x.hashCode();
//    }



}
