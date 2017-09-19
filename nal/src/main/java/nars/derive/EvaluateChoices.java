package nars.derive;

import jcog.Util;
import nars.$;
import nars.control.CauseChannel;
import nars.control.Derivation;
import org.roaringbitmap.RoaringBitmap;

import java.util.Random;
import java.util.stream.Stream;

public class EvaluateChoices extends AbstractPred<Derivation> {

    public final ValueCache values;
    public final ValueFork[] branches;
    private final CauseChannel[] causes;

    public EvaluateChoices(ValueFork[] branches) {
        super($.func("Evaluate", branches));
        this.branches = branches;
        causes = Stream.of(branches).flatMap(b -> Stream.of(b.causes)).toArray(CauseChannel[]::new);
        values = new ValueCache(c -> c::value, causes);
    }

    @Override
    public boolean test(Derivation d) {

        RoaringBitmap choices = d.choices;
        int numChoices = choices.getCardinality();
        if (numChoices == 0)
            return false;

        values.update(d.time);

        short[] routing = new short[numChoices * 2]; //sequence of (choice, score) pairs
        final int[] p = {0};
        values.getNormalized(choices, 10000, (c, v) -> {
            int pp = p[0] * 2;
            routing[pp] = (short) c;
            routing[pp + 1] = (short) v;
            p[0]++;
            return true;
        });
        if (p[0] > 1)
            bingoSortPairwise(routing);

        Random rng = d.random;
        int minPerBranch = 64;
        int maxPerBranch = 128;
        int loopCost = 5;
        //TODO int maxRepeats and associate this with expected # of termutes or something to avoid useless repeats
        int before = d.now();

        while (d.live()) {

            int sample = 0;
            if (numChoices > 1) {
                //curvebag sampling of the above array
                float x = rng.nextFloat();
                float curve = x * x * x;
                sample = (int) ((1f - curve) * (numChoices - 0.5f));
            } else {
                sample = 0;
            }

            int n = routing[sample * 2];
            int loopBudget = Util.lerp(routing[sample * 2 + 1], minPerBranch, maxPerBranch);
            int ttlSaved = d.getAndSetTTL(loopBudget) - loopBudget - loopCost;
            if (ttlSaved < 0) {
                d.setTTL(0);
                break;
            }

            branches[n].test(d); if (before > 0) d.revert(before);

            d.addTTL(ttlSaved );
        }

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

    private static void bingoSortPairwise(short[] A) {
        /*
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

    }
}

