package nars.op.data;

import nars.nal.op.BinaryTermOperator;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.container.TermContainer;
import nars.term.container.TermSet;
import org.jetbrains.annotations.NotNull;

public class union extends BinaryTermOperator {
    
    @Override public Term apply(@NotNull Term a, Term b, TermIndex i) {
        return i.the(a.op(), TermSet.union(
                (TermContainer) a, (TermContainer) b
        )).term();
    }

}
