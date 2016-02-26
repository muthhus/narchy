package nars.op.data;

import nars.nal.op.BinaryTermOperator;
import nars.term.Term;
import nars.term.TermBuilder;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class intersect extends BinaryTermOperator {

    @Nullable
    @Override public Term apply(@NotNull Term a, Term b, @NotNull TermBuilder i) {
        return i.newCompound(a.op(), TermContainer.intersect(
                (TermContainer) a, (TermContainer) b
        ));
    }

}
