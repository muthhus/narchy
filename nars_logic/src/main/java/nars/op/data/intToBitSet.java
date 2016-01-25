package nars.op.data;

import nars.$;
import nars.op.math.IntTo;
import nars.term.Compound;
import nars.term.Term;
import nars.util.data.FastBitSet;

import java.util.TreeSet;


/**
 * transforms an integer atom to a setext of atoms representing
 * the enabled bits.
 * ex:
 *
 * TODO options for selecting only on/off bits, a customizable prefix
 */
public class intToBitSet extends IntTo<Compound> {
    @Override protected Compound function(int a) {
        FastBitSet b = new FastBitSet(a);
        TreeSet<Term> bb = new TreeSet();
        for (int i=b.nextSetBit(0); i >= 0; i = b.nextSetBit(i)) {
            bb.add(bit(i));
        }
        return $.sete(bb);
    }

    private Term bit(int i) {
        return $.the(i);
    }
}
