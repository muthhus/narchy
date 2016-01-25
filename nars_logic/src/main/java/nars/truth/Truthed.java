package nars.truth;

/** indicates an implementation has, or is associated with a specific TruthValue */
public interface Truthed {
    Truth truth();

    default float expectation() {
        Truth t = truth();
        return t == null ? Float.NaN : t.expectation();
    }
    default float conf() {
        Truth t = truth();
        return t == null ? Float.NaN : t.conf();
    }
    default float freq() {
        Truth t = truth();
        return t == null ? Float.NaN : t.freq();
    }
}
