package nars.term.mutate;

import jcog.Util;
import nars.derive.meta.match.Ellipsis;
import nars.derive.meta.match.EllipsisMatch;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * choose 1 at a time from a set of N, which means iterating up to N
 */
public class Choose1 extends Termutator.AbstractTermutator {

    @NotNull
    private final Set<Term> yFree;
    private final Term x;
    private final Term xEllipsis;
    @NotNull
    private final Term[] yy;

    @Override
    protected Object newKey() {
        return Util.tuple(xEllipsis, x);
    }

    public Choose1(Ellipsis xEllipsis, Term x, @NotNull Set<Term> yFree) {
        super();

        int ysize = yFree.size();
        if (ysize < 2) {
            throw new RuntimeException(yFree + " offers no choice");
        }
        yy = yFree.toArray(new Term[ysize]);
        this.yFree = yFree;


        this.xEllipsis = xEllipsis;
        this.x = x;


    }

    @Override
    public int getEstimatedPermutations() {
        return yy.length;
    }

    @Override
    public boolean mutate(@NotNull Unify f, List<Termutator> chain, int current) {

        @NotNull Term[] yy = this.yy;

        int l = yy.length-1;
        int shuffle = f.random.nextInt(yy.length); //randomize starting offset

        int start = f.now();

        Term[] m = new Term[l];

        Term xEllipsis = this.xEllipsis;
        for (Term x = this.x; l >=0; l--) {

            Term y = next(shuffle, l);
            if (f.unify(x, y)) {
                if (f.putXY(xEllipsis, EllipsisMatch.match(TermContainer.except(yy, y, m)))) {
                    f.mutate(chain, current);
                }

            }

            if (!f.revert(start))
                return false;
        }

        return true;
    }


    private Term next(int shuffle, int permute) {
        Term[] yy = this.yy;
        return yy[(shuffle + permute) % yy.length];
    }

}
