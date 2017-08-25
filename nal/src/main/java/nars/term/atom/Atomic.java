package nars.term.atom;

import jcog.Texts;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.term.Term;
import nars.term.Termlike;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.Integer.MIN_VALUE;


/** Base class for Atomic types. */
public interface Atomic extends Term {

    @Override
    default boolean OR(Predicate<Term> v) {
        return v.test(this);
    }
    @Override
    default boolean AND(Predicate<Term> v) {
        return v.test(this);
    }

    @NotNull
    static Atomic the(@NotNull String id) {
        int l = id.length();
        if (l == 0)
            System.out.println("wtf");
        assert(l>0): "attempted zero-length Atomic id";

        //special cases
        if (l ==1) {
            switch (id.charAt(0)) {
                case Op.ImdexSym:  return Op.Imdex;
                case Op.NullSym:  return Op.Null;
                case Op.TrueSym:  return Op.True;
                case Op.FalseSym:  return Op.False;
            }
        }

        //TODO handle negative ints prefixd with '-'
        if (Character.isDigit(id.charAt(0))) {
            //try to parse int
            int i = Texts.i(id, MIN_VALUE);
            if (i != MIN_VALUE)
                return Int.the(i); //parsed as integer, so
        }

        if (isQuoteNecessary(id))
            return $.quote(id);


        return new Atom(id);
    }


    @NotNull
    @Override
    String toString();

    @Override
    default int dtRange() {
        return 0;
    }

    @Override
    default boolean recurseTerms(BiPredicate<Term, Term> whileTrue, Term parent) {
        return whileTrue.test(this, parent);
    }

    @Override
    default void recurseTerms(@NotNull Consumer<Term> v) {
        v.accept(this);
    }



    @Override
    default boolean ANDrecurse(@NotNull Predicate<Term> v) { return AND(v); }

    @Override
    default boolean ORrecurse(@NotNull Predicate<Term> v) { return AND(v); }

//    @Override
//    default String toString() {
//        return toString();
//    }




    @Override
    default void append(@NotNull Appendable w) throws IOException {
        w.append(toString());
    }

    /** number of subterms; for atoms this must be zero */
    @Override
    default int size() {
        return 0;
    }

    /** atoms contain no subterms so impossible for anything to fit "inside" it */
    @Override
    default boolean impossibleSubTermVolume(int otherTermVolume) {
        return true;
    }

    @Override
    default boolean contains(Termlike t) {
        return false;
    }

    @Override
    default boolean isCommutative() {
        return false;
    }


    /** default volume = 1 */
    @Override
    default int volume() { return 1; }

    @Override
    default Term sub(int i, Term ifOutOfBounds) {
        //no superterms to select
        return ifOutOfBounds;
    }

    @Override
    default int structure() {
        return op().bit;
    }

    @Override
    default boolean subIs(int i, Op o) {
        return false;
    }

    /**
     * determines if the string is invalid as an unquoted term according to the characters present
     */
    static boolean isQuoteNecessary(@NotNull CharSequence t) {
        int len = t.length();

        if (len > 1 && (t.charAt(0) == '\"') &&
                (t.charAt(len - 1) == '\"'))
            return false; //already quoted

        for (int i = 0; i < len; i++) {
            char c = t.charAt(i);
            if (!Narsese.isValidAtomChar(c))
                return true;
        }

        return false;
    }


}
