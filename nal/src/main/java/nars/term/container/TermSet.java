package nars.term.container;


import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermBuilder;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

public class TermSet<X extends Term> extends TermVector<X> {

    @NotNull
    public static TermSet the(Term... x) {
        return new TermSet(Terms.toSortedSetArray(x));
    }



    @NotNull
    public static TermSet the(@NotNull Collection<? extends Term> x) {
        return new TermSet(toSortedSetArray(x));
    }

    @NotNull
    public static TermContainer union(@NotNull TermContainer a, @NotNull TermContainer b) {
        if (a.equals(b))
            return a;

        int as = a.size();
        int bs = b.size();
        int maxSize = Math.max(as, bs);
        TreeSet<Term> t = new TreeSet<>();
        a.addAllTo(t);
        b.addAllTo(t);
        if (t.size() == maxSize) {
            //the smaller is contained by the larger other
            return as > bs ? a : b;
        }
        return TermSet.the(t);
    }

//    public static TermSet newTermSetPresorted(Term... presorted) {
//        return new TermSet(presorted);
//    }



    private TermSet(X[] x) {
        super(x);
    }

    @NotNull
    public static Term[] toSortedSetArray(@NotNull Collection<? extends Term> c) {

        int n = c.size();

        if (n == 0)
            return Terms.empty;

        Term[] a = c.toArray(new Term[n]);

        if (c instanceof Set) {
            if (c instanceof TreeSet) {
                //already sorted
                return a;
            } else {

                //already unique but not necessarily sorted
                if (n > 1)
                    Arrays.sort(a);

                return a;
            }
        }

        //potentially is unsorted and has duplicates
        return n > 1 ? Terms.toSortedSetArray(a) : a;
    }

    @Override public final boolean isSorted() {
        return true;
    }

    @NotNull
    @Override
    public TermVector replacing(int subterm, Term replacement) {
        throw new RuntimeException("n/a for set");
    }

    public static Compound union(TermBuilder b, Compound term1, Compound term2) {
        return union(b, term1.op(), term1, term2);
    }

    public static Compound union(TermBuilder b, Op o, Compound term1, Compound term2) {
        TermContainer u = TermSet.union(term1, term2);
        if (u == term1)
            return term1;
        else if (u == term2)
            return term2;
        else
            return (Compound) b.newCompound(o, u);
    }

}
