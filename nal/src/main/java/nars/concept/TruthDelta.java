package nars.concept;

import nars.truth.Truth;

/**
 * Records a change in truth state from before and after a task is inserteed into a belief table.
 * Used for accurate feedback measurements.
 * Recorded during a synchronized block to ensure that it assigns the credit to the inserted task accurately.
 */
public final class TruthDelta {

    public final Truth before, after;

    public TruthDelta(Truth beliefBefore, Truth beliefAfter) {
        this.before = beliefBefore;
        this.after = beliefAfter;
    }


}
