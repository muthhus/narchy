package nars.term.container;

import com.google.common.base.Joiner;
import jcog.Util;
import nars.Op;
import nars.Param;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.visit.SubtermVisitorX;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Created by me on 11/16/16.
 */
public abstract class TermVector implements TermContainer {
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
    public final byte varQuerys;
    public final byte varIndeps;
    public final byte varPatterns;
    public final byte varDeps;

    public TermVector(Term... terms) {

        if (terms.length > Param.MAX_SUBTERMS)
            throw new UnsupportedOperationException("too many subterms (" + terms.length + " > " + Param.MAX_SUBTERMS);


//         if (Param.DEBUG) {
//             for (Term x : terms)
//                 if (x == null) throw new NullPointerException();
//         }

        /**
         0: depVars
         1: indepVars
         2: queryVars
         3: patternVar
         4: volume
         5: struct
         */
        int[] meta = new int[6];
        this.hash = Terms.hashSubterms(terms, meta);

        final int vP = meta[3];  this.varPatterns = (byte)vP;   //varTot+=NO

        final int vD = meta[0];  this.varDeps = (byte)vD;
        final int vI = meta[1];  this.varIndeps = (byte)vI;
        final int vQ = meta[2];  this.varQuerys = (byte)vQ;

        int varTot = vD + vI + vQ ;

        final int vol = meta[4] + 1;
        this.volume = (short)( vol );

        final int cmp = vol - varTot - vP;
        this.complexity = (short)(cmp);

        this.structure = meta[5];
    }



    @NotNull
    public static TermVector1 the(@NotNull Term the) {
        return new TermVector1(the);
    }

    @NotNull
    public static TermVector the(@NotNull Term x, @NotNull Term y) {
        return new TermVector2(x, y);
    }


    @NotNull
    public static TermContainer the(@NotNull Term... t) {
        switch (t.length) {
            case 0:
                return Terms.NoSubterms;
            case 1:
                return the(t[0]);
            case 2:
                return the(t[0], t[1]);
            default:
                return new ArrayTermVector(t);
        }
    }

    public static TermContainer the(@NotNull Collection<? extends Term> t) {
        return TermVector.the((Term[]) t.toArray(new Term[t.size()]));
    }


    @NotNull @Override public abstract Term[] terms();


    @Override
    public void forEach(@NotNull Consumer<? super Term> action) {
        forEach(action, 0, size());
    }

//    @NotNull
//    @Override public final Term[] terms(@NotNull IntObjectPredicate<Term> filter) {
//        return Terms.filter(terms(), filter);
//    }

    @Override
    public final int structure() {
        return structure;
    }

    @Override
    @NotNull abstract public Term term(int i);

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

    @Override
    public abstract int size();

    @NotNull
    @Override
    public String toString() {
        return '(' + Joiner.on(',').join(terms()) + ')';
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
        return volume-complexity-varPatterns;
    }



    @Override
    public abstract Iterator<Term> iterator();

    @Override
    public abstract void forEach(@NotNull Consumer<? super Term> action, int start, int stop);

    @Override
    public final boolean equals(@NotNull Object obj) {
        return
            (this == obj)
            ||
            ((hash == obj.hashCode()) && (obj instanceof TermContainer) && equalTerms((TermContainer)obj));
    }


    @Override
    public final void copyInto(@NotNull Collection<Term> target) {
        Collections.addAll(target, terms());
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final int hashCodeSubTerms() {
        return hash;
    }

    public final void visit(@NotNull SubtermVisitorX v, Compound parent) {
        int cl = size();
        for (int i = 0; i < cl; i++) {
            v.accept(term(i), parent);
        }
    }

    @NotNull
    public TermContainer reverse() {
        if (size() < 2)
            return this; //no change needed

        return TermVector.the( Util.reverse( terms().clone() ) );
    }

    /** accelerated version of super-class's */
    @Override public final boolean hasAll(int equivalentSize, int baseStructure, int minVol) {
            return  (minVol <= this.volume)
                    &&
                    (equivalentSize == size())
                    &&
                    Op.hasAll(this.structure, baseStructure)
            ;
    }
}
