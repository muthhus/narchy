package nars.term.var;

import nars.$;
import nars.Op;
import nars.term.atom.AtomicString;
import org.jetbrains.annotations.NotNull;

/**
 * Unnormalized, labeled variable
 */
public class GenericVariable extends AtomicString implements Variable {

    @NotNull
    public final Op type;
    @NotNull
    public  final String label;
    @NotNull
    private final String str;

    public GenericVariable(@NotNull Op type, @NotNull String label) {
        this.label = label;
        this.type = type;
        this.str = type.ch + label;
    }

    @Override
    public final int id() {
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
        if (u instanceof GenericVariable) {
            return str.equals(((GenericVariable) u).str);
        }

        return false;
    }


    @Override
    public final int complexity() {
        return 0;
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
    public @NotNull Variable normalize(int serial) {
        return $.v(type, serial);
    }

    @NotNull
    @Override
    public final String toString() {
        return str;
    }

}
