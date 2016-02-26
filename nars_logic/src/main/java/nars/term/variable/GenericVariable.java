package nars.term.variable;

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
    public int id() {
        return 0;
    }

    @Override
    public final int complexity() {
        return 0;
    }

    @Override
    public Op op() {
        return type;
    }


    @Override
    public int varIndep() {
        return type == Op.VAR_INDEP ? 1 : 0;
    }

    @Override
    public int varDep() {
        return type == Op.VAR_DEP ? 1 : 0;
    }

    @Override
    public int varQuery() {
        return type == Op.VAR_QUERY ? 1 : 0;
    }

    @Override
    public int varPattern() {
        return type == Op.VAR_PATTERN ? 1 : 0;
    }

    @Override
    public int vars() {
        // pattern variable hidden in the count 0
        return type == Op.VAR_PATTERN ? 0 : 1;
    }

    /** produce a normalized version of this identified by the serial integer */
    public @NotNull AbstractVariable normalize(int serial) {
        return $.v(type, serial);
    }

    @NotNull
    @Override
    public String toString() {
        return str;
    }

}
