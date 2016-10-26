package nars.term.atom;

import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/** default Atom implementation */
public class Atom extends AtomicStringConstant {

    public Atom(@NotNull String id) {
        super(validateAtomID(id));
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
    public Op op() {
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

