package nars.truth;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** indicates an implementation has, or is associated with a specific TruthValue */
public interface Truthed {



    @Nullable
    Truth truth();

    /** defaults to positive */
    default float expectation() {
        //Truth t = truth();
        //return t == null ? Float.NaN : t.expectation();
        return expectation(true);
    }

    /** balanced form of expectation, where -1 = no, +1 = yes, and 0 = maybe */
    default float motivation() {
        return (freq() - 0.5f) * conf() * 2f;
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

    static float confSum(@NotNull Iterable<? extends Truthed> beliefs) {
        float t = 0;
        for (Truthed s : beliefs)
            t += s.truth().conf();
        return t;
    }

    static float freqMean(@NotNull Iterable<? extends Truthed> beliefs) {

        float t = 0;
        int count = 0;
        for (Truthed s : beliefs) {
            t += s.freq();
            count++;
        }

        return count == 0 ? 0.5f : t / count;
    }

}
