package nars.term.variable;

import nars.$;
import nars.Op;
import nars.nal.meta.AbstractLiteral;
import nars.term.Term;
import nars.term.atom.AbstractStringAtom;
import nars.term.atom.Atomic;
import nars.term.atom.StringAtom;
import org.jetbrains.annotations.NotNull;

/**
 * Unnormalized, labeled variable
 */
public class GenericVariable extends Atomic {

    private final Op type;
    private final String label;
    private final String str;

    public GenericVariable(Op type, String label) {
        this.label = label;
        this.type = type;
        this.str = type.ch + label;
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
    public @NotNull Term normalize(int serial) {
        return $.v(type, serial);
    }

    @Override
    public String toString() {
        return str;
    }

    @Override
    public final int compareTo(Object o) {
        if (o instanceof GenericVariable) {
            return str.compareTo(((GenericVariable)o).str);
        }
        return -1;
    }
}
