package nars.term.container;


import nars.term.Term;
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
    public static TermSet union(@NotNull TermContainer a, @NotNull TermContainer b) {
        TreeSet<Term> t = new TreeSet<>();
        a.addAllTo(t);
        b.addAllTo(t);
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
}
