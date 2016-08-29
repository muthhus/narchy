package nars.term.subst.choice;

import nars.util.data.list.LimitedFasterList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * appended to the end of termuator execution chains to invoke
 * any accumulated termutations occurring during the match
 * or onMatch() if it was stable
 **/
abstract public class Termunator extends Termutator {

    protected final List<Termutator> termutes;

    @NotNull
    final Termutator[] onlyThis;

    public Termunator(int stackSize) {
        super(".");
        this.termutes = new LimitedFasterList(stackSize);
        this.onlyThis = new Termutator[] { this };
    }

    @Override
    public int getEstimatedPermutations() {
        return 0;
    }
}
