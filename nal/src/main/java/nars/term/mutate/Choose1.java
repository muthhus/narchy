package nars.term.mutate;

import nars.$;
import nars.derive.meta.match.Ellipsis;
import nars.derive.meta.match.EllipsisMatch;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.SortedSet;

/**
 * choose 1 at a time from a set of N, which means iterating up to N
 */
public class Choose1 extends Termutator.AbstractTermutator {

    //@NotNull
    //private final Set<Term> yFree;
    private final Term x;
    private final Term xEllipsis;
    @NotNull
    private final Term[] yy;

    public Choose1(Ellipsis xEllipsis, Term x, @NotNull SortedSet<Term> yFree) {
        super(x, xEllipsis, $.p(yFree));

        int ysize = yFree.size();  assert(ysize >= 2): yFree + " must offer choice";

        yy = yFree.toArray(new Term[ysize]);
        //this.yFree = yFree;


        this.xEllipsis = xEllipsis;
        this.x = x;


    }

    @Override
    public int getEstimatedPermutations() {
        return yy.length;
    }

    @Override
    public void mutate(@NotNull Unify f, List<Termutator> chain, int current) {

        @NotNull Term[] yy = this.yy;

        int l = yy.length-1;
        int shuffle = f.random.nextInt(yy.length); //randomize starting offset

        int start = f.now();

        Term[] m = new Term[l];

        Term xEllipsis = this.xEllipsis;
        for (Term x = this.x; l >=0; l--) {

            Term y = this.yy[(shuffle + l) % this.yy.length];
            if (f.unify(x, y)) {
                if (f.putXY(xEllipsis, EllipsisMatch.match(TermContainer.except(yy, y, m)))) {
                    f.mutate(chain, current);
                }

            }

            if (!f.revert(start))
                break;
        }

    }


}
