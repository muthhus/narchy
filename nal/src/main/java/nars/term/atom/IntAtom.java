package nars.term.atom;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.primitives.Ints;
import jcog.byt.DynBytes;
import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.Op.INT;
import static nars.Op.Null;

/** 32-bit signed integer */
public class IntAtom implements Atomic {


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
    public @NotNull Term conceptual() {
        return Null;
    }

    @Override
    public void append(ByteArrayDataOutput out) {
        if (out instanceof DynBytes) {
            ((DynBytes)out).write(INT.id, id); //faster combo
        } else {
            out.writeByte(INT.id);
            out.writeInt(id);
        }
    }

    @Override
    public byte[] bytes() {
        return Ints.toByteArray(id);
//        if (id >= 0) {
//            if (id < 10) {
//                //fast 1-digit
//                return new byte[]{(byte) ('0' + id)};
//            } else if (id < 100) {
//                //fast 2-digit
//                return new byte[]{(byte) ('0' + (id / 10)), (byte) ('0' + (id % 10))};
//            }
//        }
//
//        return Integer.toString(id).getBytes(); //HACK TODO give IntTerm its own operator type so integer values can be stored compactly
    }

    final static int RANK = Term.opX(INT, 0);
    @Override public final int opX() {
        return RANK;
    }


    @Override
    public final int hashCode() {
        return id * 31;
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj || (obj instanceof IntAtom && id == ((IntAtom) obj).id);
    }

    @Override
    public String toString() {
        return Integer.toString(id);
    }

    @Override
    public @NotNull Op op() {
        return INT;
    }

    @Override
    public int complexity() {
        return 1;
    }
}
