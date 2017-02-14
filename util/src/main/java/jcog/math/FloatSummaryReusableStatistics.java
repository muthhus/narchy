package jcog.math;

import jcog.Util;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.FastMath;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;

import java.io.Serializable;

/**
 * floatSummaryStatistics in java.util can't be cleared
 * wtf
 * anyway we'll add stdev calcluation to this and it will
 * serve as a lighter weight replacement for Apache
 * Commons Math SummaryStatistics which also is undesirable
 *
 */
public class FloatSummaryReusableStatistics implements FloatProcedure, Serializable {
    protected long count;



    protected float sSum;
    //private float sumCompensation; // Low order bits of sum
//    private float simpleSum; // Used to compute right sum for non-finite inputs
    protected float min;
    protected float max;
    protected float mean;

    /**
     * Construct an empty instance with zero count, zero sum,
     * {@code float.POSITIVE_INFINITY} min, {@code float.NEGATIVE_INFINITY}
     * max and zero average.
     */
    public FloatSummaryReusableStatistics() {
        clear();
    }

    @Override
    public final void value(float each) {
        accept(each);
    }

    public final void clear() {
        count = 0;
        sSum = 0;
        mean = 0;
        min = Float.POSITIVE_INFINITY;
        max = Float.NEGATIVE_INFINITY;
    }
    /**
     * Records another value into the summary information.
     *
     * @param value the input value
     */
    public final void accept(float value) {

        //http://stackoverflow.com/a/36590815
        //"waldorf method"
        float tmpMean = mean;
        if (tmpMean!=tmpMean)
            mean = tmpMean = 0;
        float delta = value - tmpMean;
        mean += delta / ++count;
        sSum += delta * (value - mean);

        //sumWithCompensation(value);
        if (min > value) min = value;
        if (max < value) max = value;
    }

//    /**
//     * Incorporate a new float value using Kahan summation /
//     * compensated summation.
//     */
//    private final void sumWithCompensation(float value) {
//        float tmp = value - sumCompensation;
//        float velvel = sum + tmp; // Little wolf of rounding error
//        sumCompensation = (velvel - sum) - tmp;
//        sum = velvel;
//    }

    /**
     * Return the count of values recorded.
     *
     * @return the count of values
     */
    public final long getCount() {
        return count;
    }

    /**
     * Returns the sum of values recorded, or zero if no values have been
     * recorded.
     *
     * If any recorded value is a NaN or the sum is at any point a NaN
     * then the sum will be NaN.
     *
     * <p> The value of a floating-point sum is a function both of the
     * input values as well as the order of addition operations. The
     * order of addition operations of this method is intentionally
     * not defined to allow for implementation flexibility to improve
     * the speed and accuracy of the computed result.
     *
     * In particular, this method may be implemented using compensated
     * summation or other technique to reduce the error bound in the
     * numerical sum compared to a simple summation of {@code float}
     * values.
     *
     * @apiNote Values sorted by increasing absolute magnitude tend to yield
     * more accurate results.
     *
     * @return the sum of values, or zero if none
     */
    public final float getSum() {
        return getAverage() * count;

//        // Better error bounds to add both terms as the final sum
//        float tmp =  sum + sumCompensation;
//        if (float.isNaN(tmp) && float.isInfinite(simpleSum))
//            // If the compensated sum is spuriously NaN from
//            // accumulating one or more same-signed infinite values,
//            // return the correctly-signed infinity stored in
//            // simpleSum.
//            return simpleSum;
//        else
//            return tmp;
    }

    /**
     * Returns the minimum recorded value, {@code float.NaN} if any recorded
     * value was NaN or {@code float.POSITIVE_INFINITY} if no values were
     * recorded. Unlike the numerical comparison operators, this method
     * considers negative zero to be strictly smaller than positive zero.
     *
     * @return the minimum recorded value, {@code float.NaN} if any recorded
     * value was NaN or {@code float.POSITIVE_INFINITY} if no values were
     * recorded
     */
    public final float getMin() {
        return min;
    }

    /**
     * Returns the maximum recorded value, {@code float.NaN} if any recorded
     * value was NaN or {@code float.NEGATIVE_INFINITY} if no values were
     * recorded. Unlike the numerical comparison operators, this method
     * considers negative zero to be strictly smaller than positive zero.
     *
     * @return the maximum recorded value, {@code float.NaN} if any recorded
     * value was NaN or {@code float.NEGATIVE_INFINITY} if no values were
     * recorded
     */
    public final float getMax() {
        return max;
    }

    /**
     * Returns the arithmetic mean of values recorded, or zero if no
     * values have been recorded.
     *
     * If any recorded value is a NaN or the sum is at any point a NaN
     * then the average will be code NaN.
     *
     * <p>The average returned can vary depending upon the order in
     * which values are recorded.
     *
     * This method may be implemented using compensated summation or
     * other technique to reduce the error bound in the {@link #getSum
     * numerical sum} used to compute the average.
     *
     * @apiNote Values sorted by increasing absolute magnitude tend to yield
     * more accurate results.
     *
     * @return the arithmetic mean of values, or zero if none
     */
    public final float getAverage() {
        return mean;
    }

    /**
     * {@inheritDoc}
     *
     * Returns a non-empty string representation of this object suitable for
     * debugging. The exact presentation format is unspecified and may vary
     * between implementations and versions.
     */
    @Override
    public String toString() {
        return String.format(
                "%s{n=%d, sum=%f, min=%f, avg=%f, max=%f}",
                getClass().getSimpleName(),
                getCount(),
                getSum(),
                getMin(),
                getAverage(),
                getMax());
    }

    public final float normalize(float n) {
        float min = getMin();
        float max = getMax();
        float range = max - min;
        if (range < Float.MIN_VALUE*64f /* estimate of an FP epsilon */)
            return 0.5f;
        else
            return (n - min) / (range);
    }

    /**
     * Returns the standard deviation of the values that have been added.
     * <p>
     * Double.NaN is returned if no values have been added.
     * </p>
     * The Standard Deviation is a measure of how spread out numbers are.
     * @return the standard deviation
     */
    public float getStandardDeviation() {
        float v = getVariance();
        if (v==v)
            return (float) Math.sqrt(v);
        else
            return Float.NaN;
    }

    public float getVariance() {
        long c = count;
        if (c == 0) return Float.NaN;
        return sSum / (c);
    }

}
