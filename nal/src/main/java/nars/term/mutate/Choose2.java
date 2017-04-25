package nars.term.mutate;

import jcog.Util;
import jcog.data.array.IntArrays;
import jcog.math.Combinations;
import nars.derive.meta.match.Ellipsis;
import nars.derive.meta.match.EllipsisMatch;
import nars.term.Term;
import nars.term.container.ShuffledSubterms;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Created by me on 12/22/15.
 */
public class Choose2 extends Termutator.AbstractTermutator {

    @NotNull
    final Combinations comb;
    @NotNull
    private final Term[] yFree;
    @NotNull
    private final Term[] x;
    @NotNull
    private final Ellipsis xEllipsis;
    @NotNull
    private final Unify f;
    @NotNull
    private final ShuffledSubterms yy;

    @Override
    protected Object newKey() {
        return Util.tuple(Choose2.class,xEllipsis, x/*, yFreeSet*/);
    }

    public Choose2(@NotNull Unify f, @NotNull Ellipsis xEllipsis, @NotNull Collection<Term> x, @NotNull Collection<Term> yFreeSet) {
        super();
        this.f = f;
        this.xEllipsis = xEllipsis;
        this.x = x.toArray(new Term[x.size()]);

        int yFreeSize = yFreeSet.size();
        this.yFree = yFreeSet.toArray(new Term[yFreeSize]);
        this.yy = new ShuffledSubterms(f.random, this.yFree);

        this.comb = new Combinations(yFreeSize, 2);
    }

    @Override
    public int getEstimatedPermutations() {
        return comb.getTotal()*2;
    }

    @Override
    public boolean mutate(Unify versioneds, List<Termutator> chain, int current) {

        @NotNull Combinations ccc = this.comb;
        ccc.reset();

        boolean phase = true;

        int start = f.now();
        @NotNull ShuffledSubterms yy = this.yy;

        Term[] m = new Term[this.yy.size()-2];

        Ellipsis xEllipsis = this.xEllipsis;
        Unify f = this.f;
        Term[] x = this.x;

        int[] c = null;
        while (ccc.hasNext() || !phase) {

            c = phase ? ccc.next() : c;
            phase = !phase;

            int c0 = c[0];
            int c1 = c[1];
            IntArrays.reverse(c); //swap to try the reverse next iteration

            Term y1 = yy.sub(c0);

            if (f.unify(x[0], y1)) {

                Term y2 = yy.sub(c1);

                if (f.unify(x[1], y2) &&
                        f.putXY(xEllipsis, EllipsisMatch.match(TermContainer.except(yy, y1, y2, m)))) {

                    f.mutate(chain, current);
                }

            }

            if (!f.revert(start))
                return false;
        }

        return true;
    }

}
