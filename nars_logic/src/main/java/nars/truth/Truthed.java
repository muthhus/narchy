package nars.truth;

/** indicates an implementation has, or is associated with a specific TruthValue */
public interface Truthed {

    Truth truth();

    /** defaults to positive */
    default float expectation() {
        //Truth t = truth();
        //return t == null ? Float.NaN : t.expectation();
        return expectation(true);
    }

    default float expectation(boolean positive) {
        //Truth t = truth();
        //return t == null ? Float.NaN :
        return truth().expectation(positive);
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
