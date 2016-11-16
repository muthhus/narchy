package nars.term.container;


import nars.term.Term;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class TermSet {

    @NotNull
    public static TermContainer the(@NotNull Term... x) {
        return TermVector.the(Terms.toSortedSetArray(x));
    }

    public static TermContainer concat(@NotNull Term[] a, @NotNull Term... b) {
//        if ((a.length + b.length) == 2) {
//            //simple case
//            return TermSet.the(a[0], b[0]);
//        }
        TreeSet<Term> t = new TreeSet<>();
        Collections.addAll(t, a);
        return TermSet.concat(t, b);
    }

    public
    @NotNull
    static TermContainer concat(@NotNull TreeSet<Term> t, Term[] b) {
        Collections.addAll(t, b);
        return TermSet.the(t);
    }


    @NotNull
    public static TermContainer the(@NotNull Set<? extends Term> x) {
        return TermVector.the(toSortedSetArray(x));
    }

    //    public static TermSet newTermSetPresorted(Term... presorted) {
//        return new TermSet(presorted);
//    }




    @NotNull
    public static Term[] toSortedSetArray(@NotNull Set<? extends Term> c) {

        int n = c.size();

        if (n == 0)
            return Terms.empty;

        Term[] a = c.toArray(new Term[n]);

        //if (c instanceof Set) {

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

//        } else {
//
//            //potentially is unsorted and has duplicates
//            return n > 1 ? Terms.toSortedSetArray(a) : a;
//        }
    }


//    @NotNull
//    @Override
//    public TermVector replacing(int subterm, Term replacement) {
//        throw new RuntimeException("n/a for set");
//    }

}
