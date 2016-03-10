package nars.op.data;

import nars.$;
import nars.op.math.IntTo;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.TreeSet;
import java.util.function.IntFunction;


/**
 * transforms an integer atom to a setext of atoms representing
 * the enabled bits.
 * ex:
 *
 * TODO options for selecting only on/off bits, a customizable prefix
 */
public class intToBitSet extends IntTo<Compound> {

    public static final int bits = 8;

    @NotNull
    @Override protected Compound function(int a) {

        return the(a, this::bit);
    }

    @NotNull
    public static Compound the(int a, @NotNull IntFunction<Term> f) {
        BitSet b = BitSet.valueOf(new long[]{a});

        TreeSet<Term> bb = new TreeSet();
        for (int i=b.nextSetBit(0); i >= 0; i = b.nextSetBit(i)) {
            bb.add(f.apply(i));
            i++;
        }
        return $.sete(bb);
    }

    public static Compound the(int a) {
        return the(a, $::the);
    }

    private Term bit(int i) {
        return $.the(i);
    }
}
