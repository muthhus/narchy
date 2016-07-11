package nars.term.container;


import nars.term.Term;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TermSet<X extends Term> extends TermVector {

    @NotNull
    public static TermSet the(@NotNull Term... x) {
        return new TermSet(Terms.toSortedSetArray(x));
    }

    @NotNull public static TermSet concat(@NotNull Term[] a, @NotNull Term... b) {
        if ((a.length + b.length) == 2) {
            //simple case
            return TermSet.the(a[0], b[0]);
        }
        TreeSet<Term> t = new TreeSet<>();
        Collections.addAll(t, a);
        return concat(t, b);
    }

    public
    @NotNull
    static TermSet concat(@NotNull TreeSet<Term> t, Term[] b) {
        Collections.addAll(t, b);
        return TermSet.the(t);
    }


    @NotNull
    public static TermSet the(@NotNull Collection<? extends Term> x) {

        return new TermSet(toSortedSetArray(x));
    }

    //    public static TermSet newTermSetPresorted(Term... presorted) {
//        return new TermSet(presorted);
//    }



    private TermSet(X[] x) {
        super((Term[]) x);
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
                if (n > 1) {
                    Arrays.sort(a);
                }

                return a;
            }

        } else {

            //potentially is unsorted and has duplicates
            return n > 1 ? Terms.toSortedSetArray(a) : a;
        }
    }

    @Override public final boolean isSorted() {
        return true;
    }

//    @NotNull
//    @Override
//    public TermVector replacing(int subterm, Term replacement) {
//        throw new RuntimeException("n/a for set");
//    }

}
