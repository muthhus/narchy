package nars.term.atom;

import nars.Op;
import nars.nal.meta.AbstractLiteral;
import org.jetbrains.annotations.NotNull;

/** implemented with a native Java string.
 *  this should be the ideal choice for JDK9
 *  since it does Utf8 internally and many
 *  string operations are intrinsics.  */
public abstract class StringAtom extends AbstractLiteral {

    public final String id;

    protected StringAtom(String id) {
        this.id = id;
    }


    @Override
    public final int complexity() {
        return 1;
    }

    @Override public final String toString() {
        return id;
    }

    @NotNull
    @Override
    public final Op op() {
        return Op.ATOM;
    }


    public static final int AtomBit = Op.ATOM.bit();

    @Override
    public final int structure() {
        return AtomBit;
    }


    public final int init(int[] meta) {

        meta[4] ++;
        meta[5] |= AtomBit;

        return hashCode();
    }
}
