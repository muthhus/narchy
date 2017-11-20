package nars.term.var;

import com.google.common.io.ByteArrayDataOutput;
import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.atom.AtomicConst;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import static nars.IO.SPECIAL_OP;

/**
 * Unnormalized, labeled variable
 */
public class UnnormalizedVariable extends AtomicConst implements Variable {

    public final Op type;

    @Override public int opX() { return Term.opX(op(), 10);    }

    public UnnormalizedVariable(/*@NotNull*/ Op type, String label) {
        super(type, label);
        this.type = type;
    }

    @Override
    public final byte id() {
        throw new UnsupportedOperationException();
    }

    final @Override public boolean equals(Object u) {
        if (this == u) return true;

//        if (u instanceof AbstractVariable)
//            if (u.toString().equals(str))
//                System.out.println(this + " and " + u + " equal by string");
//            //throw new UnsupportedOperationException();
//        }

        //prevent comparison with AbstractVariable
        if (u instanceof UnnormalizedVariable) {
            return toString().equals(u.toString());
        }

        return false;
    }


    @Override
    public final int complexity() {
        return 0;
    }

    @Override
    public boolean isNormalized() {
        return false;
    }

    @NotNull
    @Override
    public final Op op() {
        return type;
    }


    @Override
    public final int varIndep() {
        return type == Op.VAR_INDEP ? 1 : 0;
    }

    @Override
    public final int varDep() {
        return type == Op.VAR_DEP ? 1 : 0;
    }

    @Override
    public final int varQuery() {
        return type == Op.VAR_QUERY ? 1 : 0;
    }

    @Override
    public final int varPattern() {
        return type == Op.VAR_PATTERN ? 1 : 0;
    }

    @Override
    public final int vars() {
        // pattern variable hidden in the count 0
        return type == Op.VAR_PATTERN ? 0 : 1;
    }

    /** produce a normalized version of this identified by the serial integer */
    @Override public Variable normalize(int serial) {
        return $.v(type, serial);
    }


//    @Override
//    public Term evalSafe(TermContext index, int remain) {
//        throw new UnsupportedOperationException();
//    }

    @Override
    public void append(ByteArrayDataOutput out) {
        out.writeByte(SPECIAL_OP);
        String s = toString();
        out.writeShort(s.length());
        out.write(s.getBytes()); //HACK
        //byte[] b = bytes();
        //out.writeShort(b.length);

    }


}
