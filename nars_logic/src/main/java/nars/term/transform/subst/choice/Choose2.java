package nars.term.transform.subst.choice;

import nars.nal.meta.match.EllipsisMatch;
import nars.term.Term;
import nars.term.container.ShuffledSubterms;
import nars.term.container.TermVector;
import nars.term.transform.subst.FindSubst;
import nars.util.data.array.IntArrays;
import nars.util.math.Combinations;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Created by me on 12/22/15.
 */
public class Choose2 extends Termutator {

    @NotNull
    final Combinations comb;
    @NotNull
    private final Set<Term> yFree;
    private final Term[] x;
    private final Term xEllipsis;
    @NotNull
    private final FindSubst f;
    @NotNull
    private final ShuffledSubterms yy;

    @NotNull
    @Override
    public String toString() {

            return "Choose2{" +
                    "yFree=" + yFree +
                    ", xEllipsis=" + xEllipsis +
                    ", x=" + x[0] + ',' + x[1] +
                    '}';

    }

    public Choose2(@NotNull FindSubst f, Term xEllipsis, Term[] x, @NotNull Set<Term> yFree) {
        super(xEllipsis);
        this.f = f;
        this.x = x;
        this.yFree = yFree;
        this.xEllipsis = xEllipsis;
        //yy = yFree.toArray(new Term[ysize]);
        yy = new ShuffledSubterms(f.random, new TermVector(yFree));
        comb = new Combinations(yy.size(), 2);
    }

    @Override
    public int getEstimatedPermutations() {
        return comb.getTotal()*2;
    }

    @Override
    public void run(FindSubst versioneds, Termutator[] chain, int current) {

        comb.reset();

        boolean state = true;

        int start = f.now();

        while (!(!(comb.hasNext() || !state))) {

            int[] c = state ? comb.next() : comb.prev();
            state = !state;

            Term y1 = yy.term(c[0]);
            int c1 = c[1];
            IntArrays.reverse(c); //swap to try the reverse next iteration

            FindSubst f = this.f;
            Term[] x = this.x;

            if (f.match(x[0], y1)) {

                Term y2 = yy.term(c1);

                if (f.match(x[1], y2) && f.putXY(xEllipsis,
                            new EllipsisMatch(yFree, y1, y2))) {

                    next(f, chain, current);
                }
            }

            f.revert(start);

        }

    }

}
