package nars.term.container;

import com.google.common.base.Joiner;
import jcog.Util;
import nars.Param;
import nars.derive.match.EllipsisMatch;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Termlike;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * what differentiates TermVector from TermContainer is that
 * a TermVector specifically for subterms.  while both
 * can be
 */
public abstract class TermVector implements TermContainer {

    /** normal high-entropy "content" hash */
    public  final int hash;
    /**
     * bitvector of subterm types, indexed by Op.id and OR'd into by each subterm
     * low-entropy, use 'hash' for normal hash operations.
     */
    public  final int structure;
    /** stored as volume+1 as if this termvector were already wrapped in its compound */
    public  final short volume;
    /** stored as complexity+1 as if this termvector were already wrapped in its compound */
    public  final short complexity;

//    /**
//     * # variables contained, of each type & total
//     * this means maximum of 127 variables per compound
//     */
//    public final byte varQuerys;
//    public final byte varIndeps;
//    public final byte varPatterns;
//    public final byte varDeps;

    protected TermVector(Term... terms) {

        assert(terms.length <= Param.COMPOUND_SUBTERMS_MAX);

//         if (Param.DEBUG) {
//             for (Term x : terms)
//                 if (x == null) throw new NullPointerException();
//         }


        this.hash = Terms.hashSubterms(terms);

//        final int vD = meta[0];  this.varDeps = (byte)vD;
//        final int vI = meta[1];  this.varIndeps = (byte)vI;
//        final int vQ = meta[2];  this.varQuerys = (byte)vQ;
//        final int vP = meta[3];  this.varPatterns = (byte)vP;   //varTot+=NO

        final int vol = 1 + Util.sum(Term::volume, terms); // meta[0] + 1;
        this.structure = Util.or(Term::structure, terms); //TermContainer.super.structure();

        int varTot = Util.sum((Term t)->(t.vars()+t.varPattern()), terms);
        final int cmp = vol - varTot;
        this.complexity = (short)(cmp);
        this.volume = (short)( vol );

    }


    @NotNull
    public static TermContainer the(Term... t) {
        for (Term x : t)
            if (x instanceof EllipsisMatch)
                throw new RuntimeException("ellipsis match should not be a subterm of ANYTHING");

        switch (t.length) {
            case 0:
                return TermContainer.NoSubterms;
            case 1:
                return new TermVector1(t[0]);
            //case 2:
                //return new TermVector2(t);
            default:
                return new ArrayTermVector(t);
        }
    }


    @Override
    public final int structure() {
        return structure;
    }

    @Override
    @NotNull abstract public Term sub(int i);

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

    @NotNull
    @Override
    public String toString() {
        return '(' + Joiner.on(',').join(toArray()) + ')';
    }


    @Override
    public abstract Iterator<Term> iterator();


    @Override
    abstract public boolean equals(Object obj);
//        return
//            (this == obj)
//            ||
//            (obj instanceof TermContainer) && equalTerms((TermContainer)obj);
//    }


    @Override
    public final int hashCodeSubTerms() {
        return hash;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

//    public final boolean visit(@NotNull BiPredicate<Term,Compound> v, Compound parent) {
//        int cl = size();
//        for (int i = 0; i < cl; i++) {
//            if (!v.test(term(i), parent))
//                return false;
//        }
//        return true;
//    }

//    @NotNull
//    public TermContainer reverse() {
//        if (size() < 2)
//            return this; //no change needed
//
//        return TermVector.the( Util.reverse( toArray().clone() ) );
//    }

}
