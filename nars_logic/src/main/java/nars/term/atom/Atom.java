package nars.term.atom;

import nars.$;
import nars.Narsese;
import nars.Op;
import nars.term.Term;
import nars.util.data.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** default Atom implementation */
public class Atom extends StringAtom {

    //private static final Map<String,Atom> atoms = Global.newHashMap();


//    public static int hash(byte[] id, int ordinal) {
//        return Util.ELFHashNonZero(id, Util.PRIME1 * (1+ordinal));
//        //return Util.WildPlasserHashNonZero(id, (1+ordinal));
//    }

    /** TODO use a hash function equivalent to String's but without allocating a String */
    @Deprecated public static int hash(@NotNull byte[] id, int ordinal) {
        return hash(new String(id).hashCode(), ordinal);
    }

    public static int hash(@NotNull String id, @NotNull Op op) {
        return hash(id.hashCode(), op);
    }

    public static int hash(int id, @NotNull Op op) {
        return hash(id, op.ordinal());
    }

    public static int hash(int id, int ordinal) {
        /* for Op.ATOM, we use String hashCode() as-is
          avoiding need to calculate or store a
          hash mutated by the Op (below) */
        if (ordinal == Op.ATOM.ordinal())
            return id;

        return Util.hashCombine(id, ordinal);
    }

    public Atom(@NotNull byte[] n) {
        super( new String(n) );
    }

    public Atom(String n) {
        super(n);//Utf8.toUtf8(n)
    }



//    /**
//     * Constructor with a given name
//     *
//     * @param id A String as the name of the Term
//     */
//    public Atom(final String id) {
//        super(id);
//    }
//
//    public Atom(final byte[] id) {
//        super(id);
//    }

    //    /**
//     * Default constructor that build an internal Term
//     */
//    @Deprecated protected Atom() {
//    }

    /** determines if the string is invalid as an unquoted term according to the characters present */
    public static boolean quoteNecessary(@NotNull CharSequence t) {
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
//            if (Character.isWhitespace(c)) return true;
            if (!Narsese.isValidAtomChar(c))
                return true;
//            if ((!Character.isDigit(c)) && (!Character.isAlphabetic(c))) return true;
        }
        return false;
    }

//    /** interns the atomic term given a name, storing it in the static symbol table */
//    public final static Atom theCached(final String name) {
//        return atoms.computeIfAbsent(name, AtomInterner);
//    }

    public static Term the(Term x) {
        return x;
    }

    @NotNull
    public static Atom the(@NotNull byte[] id) {
        return new Atom(id);
    }


    @NotNull
    public static Atom the(byte c) {
        return Atom.the(new byte[] { c });
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

    /*
    // similar to String.intern()
    public final static Atom the(final String name) {
        if (name.length() <= 2)
            return theCached(name);
        return new Atom(name);
    }
    */

    @Nullable
    public static Term the(Object o) {

        if (o instanceof Term) return ((Term)o);
        if (o instanceof String)
            return $.the((String)o);
        if (o instanceof Number)
            return $.the((Number)o);
        return null;
    }

    static final int AtomBit = Op.ATOM.bit();

    @Override
    public final int structure() {
        return AtomBit;
    }

    @NotNull
    public final String toStringUnquoted() {
        return toUnquoted(toString());
    }


    /** performs a thorough check of the validity of a term (by cloneDeep it) to see if it's valid */
//    public static boolean valid(final Term content) {
//
//        return true;
////        try {
////            Term cloned = content.cloneDeep();
////            return cloned!=null;
////        }
////        catch (Throwable e) {
////            if (Global.DEBUG && Global.DEBUG_INVALID_SENTENCES) {
////                System.err.println("INVALID TERM: " + content);
////                e.printStackTrace();
////            }
////            return false;
////        }
////
//    }
}


//    @Override
//    public boolean hasVar(Op type) {
//        return false;
//    }

//    /**
//     * Equal terms have identical name, though not necessarily the same
//     * reference.
//     *
//     * @return Whether the two Terms are equal
//     * @param that The Term to be compared with the current Term
//     */
//    @Override
//    public boolean equals(final Object that) {
//        if (this == that) return true;
//        if (!(that instanceof Atom)) return false;
//        final Atom t = (Atom)that;
//        return equalID(t);
//
////        if (equalsType(t) && equalsName(t)) {
////            t.name = name; //share
////            return true;
////        }
////        return false;
//    }


//    public final void recurseSubtermsContainingVariables(final TermVisitor v) {
//        recurseSubtermsContainingVariables(v, null);
//    }

//    /**
//     * Recursively check if a compound contains a term
//     *
//     * @param target The term to be searched
//     * @return Whether the two have the same content
//     */
//    @Override public final void recurseSubtermsContainingVariables(final TermVisitor v, Term parent) {
//        //TODO move to Variable subclass and leave this empty here
//        if (hasVar())
//            v.visit(this, parent);
//    }

//    final public byte byt(int n) {
//        return data[n];
//    }

//    final byte byt0() {
//        return data[0];
//    }

//    @Override
//    public final boolean equalsOrContainsTermRecursively(final Term target) {
//        return equals(target);
//   }

