package nars.truth;

/** indicates an implementation has, or is associated with a specific TruthValue */
public interface Truthed {
    Truth truth();

    default float getExpectation() {
        Truth t = truth();
        if (t == null) return Float.NaN;
        return t.getExpectation();
    }
    default float getConfidence() {
        Truth t = truth();
        if (t == null) return Float.NaN;
        return t.getConfidence();
    }
    default float getFrequency() {
        Truth t = truth();
        if (t == null) return Float.NaN;
        return t.getFrequency();
    }
}
