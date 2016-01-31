package nars.term.transform.subst.choice;

import nars.term.container.ShuffledSubterms;
import nars.term.container.TermContainer;
import nars.term.transform.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/22/15.
 */
public class CommutivePermutations extends Termutator {
    @NotNull
    private final ShuffledSubterms perm;
    private final TermContainer y;

    @NotNull
    @Override
    public String toString() {

            return "CommutivePermutations{" +
                    "perm=" + perm.compound +
                    ", y=" + y +
                    '}';
    }

    public CommutivePermutations(FindSubst f, @NotNull TermContainer x, @NotNull TermContainer Y) {
        super(x);
        this.y = Y;
        this.perm = new ShuffledSubterms(f.random, x);
    }

    @Override
    public int getEstimatedPermutations() {
        return perm.total();
    }

    @Override
    public void run(FindSubst f, Termutator[] chain, int current) {
        ShuffledSubterms p = this.perm;
        p.reset();
        int start = f.now();


        while (p.hasNext()) {
            p.next();
            if (f.matchLinear(p, y))
                next(f, chain, current);

            f.revert(start);
        }

    }


}
