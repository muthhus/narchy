package nars.term.mutate;

import jcog.Util;
import nars.term.container.ShuffledSubterms;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
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

    public CommutivePermutations(@NotNull Unify f, @NotNull TermContainer x, @NotNull TermContainer y) {
        super(Util.tuple(CommutivePermutations.class, x, y));
        this.y = y;
        this.perm = new ShuffledSubterms(f.random,  x);
    }

    @Override
    public int getEstimatedPermutations() {
        return perm.total();
    }

    @Override
    public boolean run(@NotNull Unify f, Termutator[] chain, int current) {
        int start = f.now();

        ShuffledSubterms p = this.perm;
        p.reset();


        while (p.hasNext()) {

            p.next();

            if (f.matchLinear(p, y)) {
                if (!f.chain(chain, current))
                    return false;
            }

            f.revert(start);
        }

        return true;
    }


}
