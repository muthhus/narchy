package nars.term.mutate;

import jcog.Util;
import nars.term.container.ShuffledSubterms;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by me on 12/22/15.
 */
public final class CommutivePermutations extends Termutator.AbstractTermutator {

    @NotNull
    private final TermContainer y;
    private final TermContainer x;

    @Override
    protected Object newKey() {
        return Util.tuple(CommutivePermutations.class, x, y);
    }

    public CommutivePermutations(@NotNull TermContainer x, @NotNull TermContainer y) {
        super();
        this.y = y;
        this.x = x;
    }

    @Override
    public int getEstimatedPermutations() {
        throw new UnsupportedOperationException();
        //return perm.total();
    }

    @Override
    public boolean mutate(@NotNull Unify f, List<Termutator> chain, int current) {
        int start = f.now();

        ShuffledSubterms p = new ShuffledSubterms(f.random,  x);
        //p.reset(f.random); //why is this needed when its called in SHuffledSubterms constructor

        while (p.hasNextThenNext()) {

            if (p.unifyLinear(y, f)) {
                f.mutate(chain, current);
            }

            if (!f.revert(start))
                return false;
        }

        return true;

    }


}
