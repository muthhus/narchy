package nars.op.data;

import nars.nal.op.BinaryTermOperator;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

public class intersect extends BinaryTermOperator {

    @Override public Term apply(@NotNull Term a, Term b, TermIndex i) {
        return i.the(a.op(),
                TermContainer.intersect(
                    (TermContainer) a, (TermContainer) b
                )
        ).term();
    }

}
