package nars.nal.meta.op;

import nars.term.Term;
import nars.term.compound.Compound;
import org.jetbrains.annotations.NotNull;

/**
 * Imdex == image index
 */
public final class ImageIndexEquals extends MatchOp {
    public final int index;

    public ImageIndexEquals(int index) {
        this.index = index;
    }

    @Override
    public boolean match(@NotNull Term t) {
        return ((Compound) t).relation() == index;
    }

    @NotNull
    @Override
    public String toString() {
        return "imdex:" + index;
    }
}
