package nars.term.atom;

import nars.Op;
import nars.term.Term;
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


    @NotNull
    public static String unquote(@NotNull Term s) {
        return toUnquoted(s.toString());
    }

    @NotNull
    public static String toUnquoted(@NotNull String x) {
        int len = x.length();
        if (len > 0 && x.charAt(0) == '\"' && x.charAt(len - 1) == '\"') {
            return x.substring(1, len - 1);
        }
        return x;
    }

}

