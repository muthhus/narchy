package nars.term.container;

import com.google.common.base.Joiner;
import nars.Op;
import nars.Param;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.visit.SubtermVisitorX;
import nars.util.Util;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Holds a vector or tuple of terms.
 * Useful for storing a fixed number of subterms
 *
 * TODO make this class immutable and term field private
 * provide a MutableTermVector that holds any write/change methods
 */
public class TermVector implements TermContainer {


    /**
     * list of (direct) term
     * TODO make not public
     */
    @NotNull
    public final Term[] terms;



    /** normal high-entropy "content" hash */
    public  final int hash;

    /**
     * bitvector of subterm types, indexed by NALOperator's .ordinal() and OR'd into by each subterm
     * low-entropy, use 'hash' for normal hash operations.
     */
    public  final int structure;


    /** stored as volume+1 as if this termvector were already wrapped in its compound */
    public  final short volume;
    /** stored as complexity+1 as if this termvector were already wrapped in its compound */
    public  final short complexity;

    /**
     * # variables contained, of each type & total
     * this means maximum of 127 variables per compound
     */
    public final byte vars;
    public final byte varQuerys;
    public final byte varIndeps;
    public final byte varPatterns;
    public final byte varDeps;


    public static TermVector the(@NotNull Collection<? extends Term> t) {
        return TermVector.the((Term[]) t.toArray(new Term[t.size()]));
    }

//    /** first n items */
//    protected TermVector(@NotNull Collection<Term> t, int n) {
//        this( t.toArray(new Term[n]));
//    }


     @SafeVarargs
     public TermVector(@NotNull Term... terms) {
        this.terms = terms;

         if (Param.DEBUG) {
             for (Term x : terms)
                 if (x == null) throw new NullPointerException();
         }
         if (terms.length > Param.MAX_SUBTERMS)
             throw new UnsupportedOperationException("too many subterms (" + terms.length + " > " + Param.MAX_SUBTERMS);

        /**
         0: depVars
         1: indepVars
         2: queryVars
         3: patternVar
         4: volume
         5: struct
         */
        int[] meta = new int[6];
        this.hash = Terms.hashSubterms(this.terms, meta);


        final int vP = meta[3];  this.varPatterns = (byte)vP;   //varTot+=NO

        final int vD = meta[0];  this.varDeps = (byte)vD;
        final int vI = meta[1];  this.varIndeps = (byte)vI;
        final int vQ = meta[2];  this.varQuerys = (byte)vQ;

        int varTot = vD + vI + vQ ;
        this.vars = (byte)(varTot);

        final int vol = meta[4] + 1;
        this.volume = (short)( vol );

        final int cmp = vol - varTot - vP;
        this.complexity = (short)(cmp);


        this.structure = meta[5];

    }



    @Override
    public final boolean isTerm(int i, @NotNull Op o) {
        return terms[i].op() == o;
    }

    @NotNull @Override public final Term[] terms() {
        return terms;
    }

    @NotNull
    @Override public final Term[] terms(@NotNull IntObjectPredicate<Term> filter) {
        return Terms.filter(terms, filter);
    }

    @NotNull
    public TermVector append(Term x) {
        return new TermVector(ArrayUtils.add(terms,x));
    }

    @Override
    public final int structure() {
        return structure;
    }

    @Override
    @NotNull public final Term term(int i) {
        return terms[i];
    }

//    public final boolean equals(Term[] t) {
//        return Arrays.equals(term, t);
//    }

    @Override
    public final int volume() {
        return volume;
    }

    /**
     * report the term's syntactic complexity
     *
     * @return the complexity value
     */
    @Override
    public final int complexity() {
        return complexity;
    }

    /**
     * get the number of term
     *
     * @return the size of the component list
     */
    @Override
    public final int size() {
        return terms.length;
    }

//
//    @Override
//    public void setNormalized(boolean b) {
//        normalized = true;
//    }


    @NotNull
    @Override
    public String toString() {
        return '(' + Joiner.on(',').join(terms) + ')';
    }

    @Override
    public final int varDep() {
        return varDeps;
    }

    @Override
    public final int varIndep() {
        return varIndeps;
    }

    @Override
    public final int varQuery() {
        return varQuerys;
    }

    @Override
    public final int varPattern() {
        return varPatterns;
    }

    @Override
    public final int vars() {
        return vars;
    }

    @Override
    public final int init(@NotNull int[] meta) {
        if (vars > 0) {
            meta[0] += varDeps;
            meta[1] += varIndeps;
            meta[2] += varQuerys;
        }

        meta[3] += varPatterns;
        meta[4] += volume;
        meta[5] |= structure;

        return 0; //hashcode for this isn't used
    }

    //    public Term[] cloneTermsReplacing(int index, Term replaced) {
//        Term[] y = termsCopy();
//        y[index] = replaced;
//        return y;
//    }



    @Override
    public final Iterator<Term> iterator() {
        return Arrays.stream(terms).iterator();
    }


    @Override
    public final void forEach(@NotNull Consumer<? super Term> action, int start, int stop) {
        Term[] tt = terms;
        for (int i = start; i < stop; i++)
            action.accept(tt[i]);
    }

    @Override
    public final void forEach(@NotNull Consumer<? super Term> action) {
        for (Term t : terms)
            action.accept(t);
    }


    @Override
    public final boolean equals(@NotNull Object obj) {
        if (this == obj)
            return true;

        if (hash!=obj.hashCode())
            return false;

        if (obj instanceof TermVector) {
            TermVector ot = (TermVector) obj;
            return Util.equals(terms, ot.terms);
        } else {
            return (obj instanceof TermContainer) && (equalTo((TermContainer) obj));
        }
    }

    /** accelerated implementation */
    @Override
    public final boolean equalTerms(@NotNull TermContainer c) {
        Term[] tt = this.terms;
        int cl = tt.length;
        for (int i = 0; i < cl; i++) {
            if (!tt[i].equals(c.term(i)))
                return false;
        }
        return true;
    }


    @Override
    public final void copyInto(@NotNull Collection<Term> target) {
        Collections.addAll(target, terms);
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    public final void visit(@NotNull SubtermVisitorX v, Compound parent) {
        for (Term t : terms)
            v.accept(t, parent);
    }

    @NotNull
    public TermVector reverse() {
        Term[] s = this.terms;
        if (s.length < 2)
            return this; //no change needed
        Term[] r = s.clone();
        ArrayUtils.reverse(r);
        return TermVector.the(r);
    }

    @NotNull
    public static TermVector the(@NotNull Term... t) {
        return t.length == 0 ? Terms.NoSubterms : new TermVector(t);
    }
    @NotNull
    public static TermVector the(@NotNull Term one) {
        return new TermVector(one);
    }


//    /** thrown if a compound term contains itself as an immediate subterm */
//    public static final class RecursiveTermContentException extends RuntimeException {
//
//        @NotNull
//        public final Term term;
//
//        public RecursiveTermContentException(@NotNull Term t) {
//            super(t.toString());
//            this.term = t;
//        }
//    }

//    /** creates a copy if changed */
//    @NotNull
//    public TermVector replacing(int subterm, @NotNull Term replacement) {
//        if (replacement.equals(term(subterm)))
//            return this;
//
//        Term[] u = term;
//        if (!u[subterm].equals(replacement)) {
//            Term[] t = u.clone();
//            t[subterm] = replacement;
//            return new TermVector(t);
//        } else
//            return this;
//
//    }


    /** accelerated version of super-class's */
    @Override public final boolean hasAll(int equivalentSize, int baseStructure, int minVol) {
            return  (minVol <= this.volume)
                    &&
                    (equivalentSize == this.terms.length)
                    &&
                    Op.hasAll(this.structure, baseStructure)
            ;
    }

}
