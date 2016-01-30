package nars.nal.meta.pre;

import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseMatch;

/**
 * After(%X,%Y) Means that
 * %X is after %Y
 * TODO use less confusing terminology and order convention
 */
public class events extends AtomicBooleanCondition<PremiseMatch> {

    public static final events the = new events();

    protected events() {
    }

    @Override
    public String toString() {
        return "events";
    }

    @Override
    public boolean booleanValueOf(PremiseMatch m) {
        return m.premise.isEvent();
    }
 }
