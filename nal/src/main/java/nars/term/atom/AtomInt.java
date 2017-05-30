package nars.term.atom;

import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.Op.ATOM;

public class AtomInt implements Atomic {

    final static int RANK = Term.opX(ATOM, 2);

    public final int id;

    final static int MAX_CACHED_INTS = 16;
    private static final AtomInt[] digits = new AtomInt[MAX_CACHED_INTS];
    static {
        for (int i = 0; i < MAX_CACHED_INTS; i++) {
            digits[i] = new AtomInt(i);
        }
    }

    public static AtomInt the(int i) {
        if (i >= 0 && i < MAX_CACHED_INTS) {
            return digits[i];
        } else {
            return new AtomInt(i);
        }
    }

    AtomInt(int i) {
        this.id = i;
    }

    @Override
    public final int opX() {
        return RANK;
    }


    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof AtomInt && id == ((AtomInt)obj).id;
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
