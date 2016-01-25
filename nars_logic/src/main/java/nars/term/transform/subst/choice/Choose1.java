package nars.term.transform.subst.choice;

import nars.nal.meta.match.EllipsisMatch;
import nars.term.Term;
import nars.term.transform.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * choose 1 at a time from a set of N, which means iterating up to N
 * will remove the chosen item(s) from Y if successful before returning
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

    public Choose1(Term xEllipsis, Term x, @NotNull Set<Term> yFree) {
        super(xEllipsis);

        int ysize = yFree.size();
        if (ysize < 2) {
            throw new RuntimeException(yFree + " offers no choice");
        }

        this.x = x;

        this.yFree = yFree;
        this.xEllipsis = xEllipsis;

        yy = yFree.toArray(new Term[ysize]);
    }

    @Override
    public int getEstimatedPermutations() {
        return yy.length;
    }

    @Override
    public void run(FindSubst f, Termutator[] chain, int current) {
        int l = yy.length-1;
        int shuffle = f.random.nextInt(l); //randomize starting offset

        int start = f.now();

        for ( ; l >=0; l--) {

            if (valid(f, next(shuffle, l))) {
                next(f, chain, current);
            }

            f.revert(start);
        }

    }


    private boolean valid(FindSubst f, Term y) {
        return f.match(x, y) && f.putXY(xEllipsis, new EllipsisMatch(yFree, y));
    }

    private Term next(int shuffle, int permute) {
        Term[] yy = this.yy;
        return yy[(shuffle + permute) % yy.length];
    }

}
