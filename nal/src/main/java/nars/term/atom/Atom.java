package nars.term.atom;

import nars.Op;
import nars.index.term.TermContext;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import static nars.Op.ATOM;

/**
 * default Atom implementation: wraps a String instance as closely as possible.
 * ideally this string is stored encoded in UTF8 byte[]'s
 */
public class Atom extends AtomicToString {

    @NotNull public final String id;

    /** (cached for speed) */
    transient final int hash;
    transient final byte[] bytes;

    protected Atom(@NotNull String id) {
        this.id = validateAtomID(id);
        this.bytes = id.getBytes();
        this.hash = super.hashCode();
    }

    @Override
    public byte[] bytes() {
        return bytes;
    }


    @NotNull
    private static String validateAtomID(@NotNull String id) {
        if (id.isEmpty())
            throw new UnsupportedOperationException("Empty Atom ID");

        char c = id.charAt(0);
        switch (c) {
            case '+':
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':

            case '^':
            case '?':
            case '%':
            case '#':
            case '$':
                throw new RuntimeException("invalid " + Atom.class + " name \"" + id + "\": leading character imitates another operation type");
        }

        //return id.intern();
        return id;
    }

    public final static int AtomOpX = Term.opX(ATOM, 1);
    @Override public final int opX() {
        return AtomOpX;
    }

    @NotNull
    @Override
    public final Op op() {
        return Op.ATOM;
    }

    @Override
    public Term eval(TermContext index) {
        Termed existing = index.get(this); //resolve atoms to their concepts for efficiency
        //assumes the AtomConcept returned is the Term itself, as .term() would return
        return existing != null ? existing.term() : this;
    }

    @NotNull
    @Override public final String toString() {
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

    @Override
    public final int vars() {
        return 0;
    }


    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final void init(@NotNull int[] meta) {

        meta[4] ++; //volume
        meta[5] |= op().bit; //structure();

    }

}

