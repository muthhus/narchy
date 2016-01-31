package nars.nal.meta.pre;

import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseMatch;

/**
 * True if the premise task and belief are both non-eternal events
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
