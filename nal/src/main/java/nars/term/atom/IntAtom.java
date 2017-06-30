package nars.term.atom;

import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.Op.ATOM;

public class IntAtom implements Atomic {

    final static int RANK = Term.opX(ATOM, 2);

    public final int id;

    final static int MAX_CACHED_INTS = 16;
    private static final IntAtom[] digits = new IntAtom[MAX_CACHED_INTS];
    static {
        for (int i = 0; i < MAX_CACHED_INTS; i++) {
            digits[i] = new IntAtom(i);
        }
    }

    public static IntAtom the(int i) {
        if (i >= 0 && i < MAX_CACHED_INTS) {
            return digits[i];
        } else {
            return new IntAtom(i);
        }
    }

    IntAtom(int i) {
        this.id = i;
    }

    @Override
    public byte[] bytes() {
        if (id >= 0 && id < 10) {
            //HACK fast 1-digit
            return new byte[] {(byte) ('0' + id)};
        } //TODO fast 2-digit

        return Integer.toString(id).getBytes(); //HACK TODO give IntTerm its own operator type so integer values can be stored compactly
    }

    @Override
    public final int opX() {
        return RANK;
    }


    @Override
    public final int hashCode() {
        return id * 31;
    }

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof IntAtom && id == ((IntAtom)obj).id;
    }

    @Override public String toString() {
        return Integer.toString(id);
    }

    @Override
    public @NotNull Op op() {
        return ATOM;
    }

    @Override
    public int complexity() {
        return 1;
    }
}
