package nars.nal.meta.op;

import nars.nal.meta.TaskBeliefSubterms;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Created by me on 12/17/15.
 */
public final class TermNotEquals extends SubtermPathCondition {

    /** TODO the shorter path should be set for 'a' if possible, because it will be compared first */
    protected TermNotEquals(int a, byte[] aPath, int b, byte[] bPath) {
        super(aPath, a, bPath, b);
    }

    @NotNull
    public static TermNotEquals the(@NotNull TaskBeliefSubterms p) {
        if (p.a < p.b) {
            return new TermNotEquals(p.a, p.aPath, p.b, p.bPath);
        } else if (p.a > p.b) {
            return new TermNotEquals(p.b, p.bPath, p.a, p.aPath);
        } else {
            //sort by the path
            int pc = Arrays.compare(p.aPath, p.bPath);
            if (pc < 0) {
                return new TermNotEquals(p.a, p.aPath, p.b, p.bPath);
            } else if (pc > 0) {
                return new TermNotEquals(p.b, p.bPath, p.a, p.aPath);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }


    @Override
    protected boolean eval(@NotNull Term a, Term b) {
        return !a.equals(b);
    }

}
