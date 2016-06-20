package nars.term.subst.choice;

import nars.nal.meta.match.Ellipsis;
import nars.nal.meta.match.EllipsisMatch;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * choose 1 at a time from a set of N, which means iterating up to N
 */
public class Choose1 extends Termutator {

    @NotNull
    private final Set<Term> yFree;
    private final Term x;
    private final Term xEllipsis;
    @NotNull
    private final Term[] yy;

    @NotNull
    @Override
    public String toString() {

        return "Choose1{" +
                "yFree=" + yFree +
                ", xEllipsis=" + xEllipsis +
                ", x=" + x +
                '}';
    }

    public Choose1(Ellipsis xEllipsis, Term x, @NotNull Set<Term> yFree) {
        super(xEllipsis);



        int ysize = yFree.size();
        if (ysize < 2) {
            throw new RuntimeException(yFree + " offers no choice");
        }

        this.xEllipsis = xEllipsis;
        this.x = x;

        this.yFree = yFree;

        yy = yFree.toArray(new Term[ysize]);
    }

    @Override
    public int getEstimatedPermutations() {
        return yy.length;
    }

    @Override
    public void run(@NotNull FindSubst f, Termutator[] chain, int current) {

        @NotNull Term[] yy = this.yy;

        int l = yy.length-1;
        int shuffle = f.random.nextInt(l); //randomize starting offset

        int start = f.now();

        Term[] m = new Term[l];

        Term xEllipsis = this.xEllipsis;
        for (Term x = this.x; l >=0; l--) {

            Term y = next(shuffle, l);
            if (f.match(x, y)) {
                if (f.putXY(xEllipsis, EllipsisMatch.match(TermContainer.except(yy, y, m)))) {
                    next(f, chain, current);
                }

                f.revert(start);
            }
        }

    }


    private Term next(int shuffle, int permute) {
        Term[] yy = this.yy;
        return yy[(shuffle + permute) % yy.length];
    }

}
