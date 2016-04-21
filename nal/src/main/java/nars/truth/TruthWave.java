package nars.truth;

import nars.NAR;
import nars.concept.table.BeliefTable;
import nars.nal.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/** compact chart-like representation of a belief state at each time cycle in a range of time.
 *  useful as a memoized state snapshot of a belief table
 *  stored in an array of float quadruplets for each task:
 *      1) freq
 *      2) conf
 *      3) occ
 *      4) pri
 * */
public class TruthWave {

    /** start and stop interval (in cycles) */
    long start;
    long end;

    /** sequence of triples (freq, conf, occurr) for each task; NaN for eternal */
    float[] truth;
    int size;
    @Nullable
    public Truth current;

    public TruthWave(int initialCapacity) {
        resize(initialCapacity);
        clear();
    }

    private void clear() {
        size = 0;
        start = end = Tense.ETERNAL;
    }

    private void resize(int cap) {
        truth = new float[4*cap];
    }

    public TruthWave(@NotNull BeliefTable b, @NotNull NAR n) {
        this(b.size());
        set(b, n.time());
    }

    /** clears and fills this wave with the data from a table */
    public void set(@NotNull BeliefTable b, long now) {
        if (b.isEmpty()) {
            clear();
            return;
        }

        int s = b.size();
        int c = capacity();

        if (c < s)
            resize(s);
        else {
            if (s < c) Arrays.fill(truth, 0); //TODO memfill only the necessary part of the array that won't be used
        }

        float[] t = this.truth;


        final int[] size = {0};
        b.forEach(x -> {
            int j = size[0] * 4;
            t[j++] = x.freq();
            t[j++] = x.conf();
            long occ = x.occurrence();
            t[j++] = occ==Tense.ETERNAL ? Float.NaN : occ;
            t[j/*++*/] = x.pri();
            size[0]++;
        });
        this.size = size[0];

        //compute time range
        float start = Float.POSITIVE_INFINITY;
        float end = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < size[0]; i++) {
            float o = t[i*4 + 2];
            if (o > end) end = o;
            if (o < start) start = o;
        }
        this.start = (long)start;
        this.end = (long)end;

        this.current = b.truth(now);
    }

    public boolean isEmpty() { return size == 0; }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    @FunctionalInterface public interface TruthWaveVisitor {
        void onTruth(float f, float c, float occ, float pri);
    }

    public final void forEach(@NotNull TruthWaveVisitor v) {
        int s = this.size;
        float[] t = this.truth;
        int j = 0;
        for (int i = 0; i < s; i++) {
            float f = t[j++];
            float c = t[j++];
            float o = t[j++];
            float p = t[j++];
            v.onTruth(f, c, o, p);
        }
    }

    public final int capacity() { return truth.length / 4; }

//        //get min and max occurence time
//        for (Task t : beliefs) {
//            long o = t.occurrence();
//            if (o == Tense.ETERNAL) {
//                expectEternal1 += t.truth().expectationPositive();
//                expectEternal0 += t.truth().expectationNegative();
//                numEternal++;
//            }
//            else {
//                numTemporal++;
//                if (o > max) max = o;
//                if (o < min) min = o;
//            }
//        }
//
//        if (numEternal > 0) {
//            expectEternal1 /= numEternal;
//            expectEternal0 /= numEternal;
//        }
//
//        start = min;
//        end = max;
//
//        int range = length();
//        expect = new float[2][];
//        expect[0] = new float[range+1];
//        expect[1] = new float[range+1];
//
//        if (numTemporal > 0) {
//            for (Task t : beliefs) {
//                long o = t.occurrence();
//                if (o != Tense.ETERNAL) {
//                    int i = (int)(o - min);
//                    expect[1][i] += t.truth().expectationPositive();
//                    expect[0][i] += t.truth().expectationNegative();
//                }
//            }
//
//            //normalize
//            for (int i = 0; i < (max-min); i++) {
//                expect[0][i] /= numTemporal;
//                expect[1][i] /= numTemporal;
//            }
//        }
//
//    }
//
//    //TODO getFrequencyAnalysis
//    //TODO getDistribution
//
//    public int length() { return (int)(end-start); }
//
//    public void print() {
//        System.out.print("eternal=" + numEternal + ", temporal=" + numTemporal);
//
//
//        if (length() == 0) {
//            System.out.println();
//            return;
//        }
//        System.out.println(" @ " + start + ".." + end);
//
//        for (int c = 0; c < 2; c++) {
//            for (int i = 0; i < length(); i++) {
//
//                float v = expect[c][i];
//
//                System.out.print(Util.n2u(v) + ' ');
//
//            }
//            System.out.println();
//        }
//    }


    @NotNull
    @Override
    public String toString() {
        return start() + ".." + end() + ": " + Arrays.toString(truth);
    }
}
