package nars.term.mutate;

import org.jetbrains.annotations.NotNull;

/**
 * appended to the end of termuator execution chains to invoke
 * any accumulated termutations occurring during the match
 * or onMatch() if it was stable
 **/
abstract public class Termunator extends Termutator {


    @NotNull
    final Termutator[] onlyThis;

    public Termunator() {
        super(Termunator.class);
        this.onlyThis = new Termutator[] { this };
    }

    @Override
    public int getEstimatedPermutations() {
        return 0;
    }
}
