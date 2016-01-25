package nars.truth;

/** indicates an implementation has, or is associated with a specific TruthValue */
public interface Truthed {
    Truth truth();

    default float expectation() {
        Truth t = truth();
        if (t == null) return Float.NaN;
        return t.getExpectation();
    }
    default float conf() {
        Truth t = truth();
        if (t == null) return Float.NaN;
        return t.conf();
    }
    default float freq() {
        Truth t = truth();
        if (t == null) return Float.NaN;
        return t.freq();
    }
}
