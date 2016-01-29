package nars.budget;

import nars.Global;
import nars.util.data.Util;

/**
 * Budget merge function, with input scale factor
 */
@FunctionalInterface
public interface BudgetMerge {

    BudgetMerge plusDQBlend = (tgt, src, srcScale) -> {
        float dp = src.pri() * srcScale;

        float currentPriority = tgt.priIfFiniteElseZero();

        float nextPri = currentPriority + dp;
        if (nextPri > 1) nextPri = 1f;

        float currentNextPrioritySum = currentPriority + nextPri;

        /* current proportion */
        float cp = currentNextPrioritySum != 0 ? currentPriority / currentNextPrioritySum : 0.5f;

        /* next proportion = 1 - cp */
        float np = 1.0f - cp;

        float nextDur = cp * tgt.dur() + np * src.dur();
        float nextQua = cp * tgt.qua() + np * src.qua();

        assert Float.isFinite(nextDur) : "NaN dur: " + src + ' ' + tgt.dur();
        assert Float.isFinite(nextQua) : "NaN quality";

        tgt.budget( nextPri,nextDur,nextQua);
    };

    /** merge 'incoming' budget (scaled by incomingScale) into 'existing' */
    void merge(Budget existing, Budget incoming, float incomingScale);


    BudgetMerge plusDQDominant = (tgt, src, srcScale) -> {
        float nextPriority = src.pri() * srcScale;

        float currentPriority = tgt.priIfFiniteElseZero();

        float sumPriority = currentPriority + nextPriority;
        if (sumPriority > 1) sumPriority = 1f;

        boolean currentWins = currentPriority > nextPriority;

        tgt.budget( sumPriority,
                (currentWins ? tgt.dur() : src.dur()),
                (currentWins ? tgt.qua() : src.qua()));
    };

//    /** add priority, interpolate durability and quality according to the relative change in priority
//     *  WARNING untested
//     * */
//    BudgetMerge plusDQInterp = (tgt, src, srcScale) -> {
//        float dp = src.pri() * srcScale;
//
//        float currentPriority = tgt.priIfFiniteElseZero();
//
//        float nextPri = currentPriority + dp;
//        if (nextPri > 1) nextPri = 1f;
//
//        float currentNextPrioritySum = (currentPriority + nextPri);
//
//        /* current proportion */
//        final float cp = currentNextPrioritySum != 0 ? currentPriority / currentNextPrioritySum : 0.5f;
//
//        /* next proportion = 1 - cp */
//        float np = 1.0f - cp;
//
//
//        float nextDur = (cp * tgt.dur()) + (np * src.dur());
//        float nextQua = (cp * tgt.qua()) + (np * src.qua());
//
//        if (!Float.isFinite(nextDur))
//            throw new RuntimeException("NaN dur: " + src + ' ' + tgt.dur());
//        if (!Float.isFinite(nextQua))
//            throw new RuntimeException("NaN quality");
//
//        tgt.budget( nextPri, nextDur, nextQua );
//    };

    /** add priority, interpolate durability and quality according to the relative change in priority
     *  WARNING untested
     * */
    BudgetMerge max = (tgt, src, srcScaleIgnored) -> {
        tgt.budget(
                Util.max(src.priIfFiniteElseZero(), tgt.priIfFiniteElseZero()),
                Util.max(src.dur(), tgt.dur()),
                Util.max(src.qua(), tgt.qua()));
    };

//    /** the max priority, durability, and quality of two tasks */
//    default Budget mergeMax(Budget b) {
//        return budget(
//                Util.max(getPriority(), b.getPriority()),
//                Util.max(getDurability(), b.getDurability()),
//                Util.max(getQuality(), b.getQuality())
//        );
//    }

    /** LERP average proportional to priority change */
    BudgetMerge avg = (tgt, src, srcScaleIgnored) -> {

        float currentPriority = tgt.pri();

        float otherPriority = src.pri();

        float prisum = (currentPriority + otherPriority);


        /* current proportion */
        float cp = (Util.equal(prisum, 0, Global.BUDGET_PROPAGATION_EPSILON)) ?
                0.5f : /* both are zero so they have equal infleunce */
                (currentPriority / prisum);

        /* next proportion */
        float np = 1.0f - cp;

        tgt.budget(
                cp * currentPriority + np * otherPriority,
                cp * tgt.dur() + np * src.dur(),
                cp * tgt.qua() + np * src.qua()
        );
    };
//    /**
//     * merges another budget into this one, averaging each component
//     */
//    default void mergeAverageLERP(Budget that) {

//    }

//    /* ----------------------- Concept ----------------------- */
//    /**
//     * Activate a concept by an incoming TaskLink
//     *
//     *
//     * @param factor linear interpolation factor; 1.0: values are applied fully,  0: values are not applied at all
//     * @param receiver The budget receiving the activation
//     * @param amount The budget for the new item
//     */
//    public static void activate(final Budget receiver, final Budget amount, final Activating mode, final float factor) {
//        switch (mode) {
//            /*case Max:
//                receiver.max(amount);
//                break;*/
//
//            case Accum:
//                receiver.accumulate(amount);
//                break;
//
//            case Classic:
//                float priority = or(receiver.getPriority(), amount.getPriority());
//                float durability = aveAri(receiver.getDurability(), amount.getDurability());
//                receiver.setPriority(priority);
//                receiver.setDurability(durability);
//                break;
//
//            case WTF:
//
//                final float currentPriority = receiver.getPriority();
//                final float targetPriority = amount.getPriority();
//                /*receiver.setPriority(
//                        lerp(or(currentPriority, targetPriority),
//                                currentPriority,
//                                factor) );*/
//                float op = or(currentPriority, targetPriority);
//                if (op > currentPriority) op = lerp(op, currentPriority, factor);
//                receiver.setPriority( op );
//
//                final float currentDurability = receiver.getDurability();
//                final float targetDurability = amount.getDurability();
//                receiver.setDurability(
//                        lerp(aveAri(currentDurability, targetDurability),
//                                currentDurability,
//                                factor) );
//
//                //doesnt really change it:
//                //receiver.setQuality( receiver.getQuality() );
//
//                break;
//        }
//
//    }
//

//    /**
//     * merges another budget into this one, averaging each component
//     */
//    public void mergeAverage(@NotNull Budget that) {
//        if (this == that) return;
//
//        budget(
//                mean(pri(), that.pri()),
//                mean(dur(), that.dur()),
//                mean(qua(), that.qua())
//        );
//    }

}
