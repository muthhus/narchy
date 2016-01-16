package nars.nal.op;

import nars.term.Term;
import nars.term.TermBuilder;
import nars.term.TermContainer;
import nars.term.TermSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class union extends BinaryTermOperator {
    
    @Nullable
    @Override public Term apply(@NotNull Term a, Term b, @NotNull TermBuilder i) {
        return i.newTerm(a.op(), TermSet.union(
                (TermContainer) a, (TermContainer) b
        ));
    }

}
