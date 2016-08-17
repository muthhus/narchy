package nars.term.subst.choice;

import nars.Param;
import nars.term.container.ShuffledSubterms;
import nars.term.container.TermContainer;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/22/15.
 */
public final class CommutivePermutations extends Termutator {
    @NotNull
    private final ShuffledSubterms perm;
    @NotNull
    private final TermContainer y;

    @NotNull
    @Override
    public String toString() {

            return "CommutivePermutations{" +
                    "perm=" + perm.srcsubs +
                    ", y=" + y +
                    '}';
    }

    public CommutivePermutations(@NotNull FindSubst f, @NotNull TermContainer x, @NotNull TermContainer Y) {
        super(x);
        this.y = Y;
        this.perm = new ShuffledSubterms(f.random,  x);
    }

    @Override
    public int getEstimatedPermutations() {
        return perm.total();
    }

    @Override
    public void run(@NotNull FindSubst f, Termutator[] chain, int current) {
        int start = f.now();

        ShuffledSubterms p = this.perm;
        p.reset();


        while (p.hasNext()) {

            p.next();

            if (f.matchLinear(p, y))
                next(f, chain, current);

            f.revert(start);
        }

    }


}
