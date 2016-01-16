package nars.nal.op;

import nars.term.Term;
import nars.term.TermContainer;
import nars.term.compile.TermBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class intersect extends BinaryTermOperator {

    @Nullable
    @Override public Term apply(@NotNull Term a, Term b, @NotNull TermBuilder i) {
        return i.newTerm(a.op(), TermContainer.intersect(
                (TermContainer) a, (TermContainer) b
        ));
    }

}
