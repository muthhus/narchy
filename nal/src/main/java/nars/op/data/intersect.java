package nars.op.data;

import com.gs.collections.api.set.MutableSet;
import nars.nal.op.BinaryTermOperator;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

public class intersect extends BinaryTermOperator {

    @Override public Term apply(@NotNull Term a, Term b, TermIndex i) {
        MutableSet<Term> s = TermContainer.intersect(
                (TermContainer) a, (TermContainer) b
        );
        return s.isEmpty() ? null : i.the(a.op(), s).term();
    }

}
