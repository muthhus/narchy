package nars.derive;

import jcog.Util;
import nars.$;
import nars.Param;
import nars.control.Cause;
import nars.control.Derivation;
import org.roaringbitmap.IntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

/** set of branches, subsets of which a premise "can" "try" */
public class Try extends AbstractPred<Derivation> {

    public final ValueCache values;
    public final PrediTerm<Derivation>[] branches;
    private final Cause[] causes;

    Try(PrediTerm<Derivation>[] branches, Cause[] causes) {
        super($.func("try", branches));
        this.branches = branches;
        this.causes = causes;
        values = new ValueCache(c -> c::value, causes);
    }

    public Try(ValueFork[] branches) {
        this(branches, Stream.of(branches).flatMap(b -> Stream.of(b.causes)).toArray(Cause[]::new));
    }

    @Override
    public PrediTerm<Derivation> transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
        return new Try(
            PrediTerm.transform(f, branches), causes
        );
    }

    @Override
    public boolean test(Derivation d) {

        RoaringBitmap choices = d.preToPost;
        int numChoices = choices.getCardinality();
        if (numChoices == 0)
            return false;

        values.update(d.time);

        short[] routing = new short[numChoices * 2]; //sequence of (choice, score) pairs
        final int[] p = {0};

        Random rng = d.random;
        IntIterator ii = numChoices==1 || rng.nextBoolean() ? choices.getIntIterator() : choices.getReverseIntIterator();
        values.getNormalized(ii, 10, (c, v) -> {
            int pp = p[0]++ * 2;
            routing[pp] = (short) c;
            routing[pp + 1] = (short) v;
            return true;
        });

        d.preToPost.clear();

        int minVal, maxVal;
        float valRatio;
        if (p[0] > 1) {
            int[] minMax = new int[2];
            bingoSortPairwise(routing, minMax);
            minVal = minMax[0]; maxVal = minMax[1];
            valRatio = minVal/((float)(maxVal));
        } else {
            minVal = maxVal = routing[1];
            valRatio = 1f;
        }

        //TODO fork budgeting
        @Deprecated int loopCost = 1;

        int startTTL = d.ttl;
        int minPerBranch =
                Math.min(startTTL,
                    Param.TTL_PREMISE_MIN
                );

        int maxPerBranch = Math.max(minPerBranch, startTTL / numChoices);
        int before = d.now();
        int ttlSaved;
        do {

            int sample;
            if (numChoices > 1) {
                if (minVal == maxVal) {
                    //flat
                    sample = rng.nextInt(numChoices);
                } else {
                    //curvebag sampling of the above array
                    float x = rng.nextFloat();
                    float curve = Util.lerp(x, x*x, valRatio);
                    sample = (int) ((1f - curve) * (numChoices - 0.5f));
                    if (sample >= numChoices) sample = numChoices-1; //HACK happens rarely, rounding error?
                }
            } else {
                sample = 0;
            }

//            if (d.ttl <= loopCost) {
//                d.setTTL(0);
//                break;
//            }
            int n = g2(routing, sample, KEY);
            float branchScore =
                    minVal!=maxVal ? ((float) (g2(routing, sample, VAL)) - minVal) / (maxVal - minVal) : 0.5f;
            int loopBudget = Util.lerp(branchScore, minPerBranch, maxPerBranch);

            ttlSaved = d.getAndSetTTL(loopBudget) - loopBudget - loopCost;

            branches[n].test(d);

            if (before > 0) d.revert(before);

        } while (d.addTTL(ttlSaved) >= 0);

        return false;
    }


    /**
     * get
     */
    private static short g2(short[] s, int i, boolean firstOrSecond) {
        return s[i * 2 + (firstOrSecond ? 0 : 1)];
    }

    /**
     * put
     */
    private static short p2(short[] s, int i, boolean firstOrSecond, short newValue) {
        int ii = i * 2 + (firstOrSecond ? 0 : 1);
        short prev = s[ii];
        s[ii] = newValue;
        return prev;
    }

    private static final boolean KEY = true;
    private static final boolean VAL = false;

    /**
     * set
     */
    private static void s2(short[] s, int i, boolean firstOrSecond, short newValue) {
        s[i * 2 + (firstOrSecond ? 0 : 1)] = newValue;
    }

    private static void bingoSortPairwise(short[] A, int[] range) {
        /*
        https://en.wikipedia.org/wiki/Selection_sort
        In the bingo sort variant, items are ordered by repeatedly
        looking through the remaining items to find the greatest
        value and moving all items with that value to their final location.
        [2] Like counting sort, this is an efficient variant if
        there are many duplicate values.
        Indeed, selection sort does one pass through
        the remaining items for each item moved.
        Bingo sort does one pass for each value (not item):
        after an initial pass to find the biggest value,
        the next passes can move every item with that value to
        its final location while finding the next value
*/
//{ This procedure sorts in ascending order. }
        int max = (A.length - 1) / 2;

    /* The first iteration is written to look very similar to the subsequent ones, but
      without swaps. */
        short vm = g2(A, max, VAL); //   nextValue := A[max];
        for (int i = max - 1; i >= 0; i--) { //    for i := max - 1 downto 0 do
            short vi;
            if ((vi = g2(A, i, VAL)) > vm) //        if A[i] > nextValue then
                vm = vi;
        }
        range[1] = vm;
        while (max >= 0 && g2(A, max, VAL) == vm) max--;
        while (max >= 0) { //    while max > 0 do begin
            float value = vm;
            vm = g2(A, max, VAL);
            for (int i = max - 1; i >= 0; i--) {  //for i:=max - 1 downto 0 do
                short vi = g2(A, i, VAL);
                if (vi == value) {
                    //swap(A[i], A[max]);
                    short ki = g2(A,i,KEY);
                    short km = g2(A,max,KEY);
                    s2(A,i,KEY, km);
                    s2(A,i,VAL, vm);
                    s2(A,max,KEY, ki);
                    s2(A,max,VAL, vi);
                    max--;
                } else if (vi > vm)
                    vm = vi;
            }
            while (max >= 0 && g2(A, max, VAL) == vm)
                max--;
        }
        range[0] = g2(A, 0, VAL);

    }
}

