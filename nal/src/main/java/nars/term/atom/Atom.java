package nars.term.atom;

import nars.Op;
import nars.index.term.TermIndex;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

/** default Atom implementation: wraps a String instance as closely as possible.
 * ideally this string is stored encoded in UTF8 byte[]'s */
public class Atom extends AtomicString {

    private final String id;

    public Atom(@NotNull String id) {

        this.id = (validateAtomID(id));
    }

    @Override
    public final String toString() {
        return id;
    }

    @Override
    public final int varIndep() {
        return 0;
    }

    @Override
    public final int varDep() {
        return 0;
    }

    @Override
    public final int varQuery() {
        return 0;
    }

    @Override
    public final int varPattern() {
        return 0;
    }

    @NotNull
    private static String validateAtomID(@NotNull String id) {
        if (id.isEmpty())
            throw new UnsupportedOperationException("Empty Atom ID");

        char c = id.charAt(0);
        switch (c) {
            case '^':
            case '?':
            case '%':
            case '#':
            case '$':
                throw new RuntimeException("invalid Atom name \"" + id + "\": leading character imitates another operation type");
        }

        //return id.intern();
        return id;
    }

    @NotNull
    @Override
    public final Op op() {
        return Op.ATOM;
    }

    @Override public Term eval(TermIndex index) {
        Termed existing = index.get(this); //resolve atoms to their concepts for efficiency
        if (existing != null)
            return existing.term(); //assumes the AtomConcept returned is the Term itself, as .term() would return
        return this;
    }
}

