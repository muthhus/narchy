package nars.derive.mutate;

import nars.$;
import nars.derive.match.Ellipsis;
import nars.derive.match.EllipsisMatch;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

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
    public void mutate(@NotNull Unify u, Termutator[] chain, int current) {

        @NotNull Term[] yy = this.yy;

        int l = yy.length-1;
        int shuffle = u.random.nextInt(yy.length); //randomize starting offset

        int start = u.now();

        Term[] m = new Term[l];

        Term xEllipsis = this.xEllipsis;
        for (Term x = this.x; l >=0; l--) {

            Term y = this.yy[(shuffle + l) % this.yy.length];
            if (x.unify(y, u)) {
                if (xEllipsis.unify(EllipsisMatch.match(TermContainer.exceptThe(yy, y, m)), u)) {
                    u.tryMutate(chain, current);
                }

            }

            if (!u.revertAndContinue(start))
                break;
        }

    }


}
