//package nars.truth;
//
//import nars.Global;
//import nars.util.data.Util;
//import org.jetbrains.annotations.NotNull;
//
//
//public abstract class AbstractTruth<T> implements MetaTruth<T> {
//
//    /**
//     * The confidence factor of the truth value
//     */
//    public final float conf;
//
//    public AbstractTruth(float conf) {
//        this.conf = conf;
//    }
//
//    @Override
//    public final float conf() {
//        return conf;
//    }
//
//
//
//    /**
//     * Compare two truth values
//     *
//     * @param that The other TruthValue
//     * @return Whether the two are equivalent
//     */
//
//    @Override
//    public boolean equals(Object that) {
//        return (that == this) ||
//               ((that instanceof Truth) && equalsTruth((Truth)that));
//    }
//
//    protected final boolean equalsTruth(@NotNull Truth t) {
//        return equalsConfidence(t) && equalsFrequency(t);
//    }
//
//    protected final boolean equalsConfidence(@NotNull Truth t) {
//        return Util.equals(conf, t.conf(), Global.TRUTH_EPSILON);
//    }
//
//    protected abstract boolean equalsFrequency(Truth t);
//
//
//
//    /**
//     * The String representation of a TruthValue, as used internally by the system
//     *
//     * @return The String
//     */
//    @NotNull
//    @Override
//    public String toString() {
//        //return DELIMITER + frequency.toString() + SEPARATOR + confidence.toString() + DELIMITER;
//
//        //1 + 6 + 1 + 6 + 1
//        return toCharSequence().toString();
//    }
//
//}
