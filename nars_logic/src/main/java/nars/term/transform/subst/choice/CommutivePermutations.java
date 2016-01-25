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
    final ShuffledSubterms perm;
    private final TermContainer y;
    @NotNull
    private final FindSubst f;

    @NotNull
    @Override
    public String toString() {

            return "CommutivePermutations{" +
                    "perm=" + perm.compound +
                    ", y=" + y +
                    '}';
    }

    public CommutivePermutations(@NotNull FindSubst f, TermContainer x, TermContainer Y) {
        super(x);
        this.perm = new ShuffledSubterms(f.random, x);
        this.y = Y;
        this.f = f;
    }

    @Override
    public int getEstimatedPermutations() {
        return perm.total();
    }

    @Override
    public boolean next() {
        perm.next();
        return f.matchLinear(perm, y);
    }

    @Override
    public void reset() {
        perm.reset();
    }

    @Override
    public boolean hasNext() {
        return perm.hasNext();
    }
}
