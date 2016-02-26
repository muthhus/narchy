package nars.op.data;

import nars.nal.op.BinaryTermOperator;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

public class differ extends BinaryTermOperator/*implements BinaryOperator<Term>*/ {

    @Override
    public Term apply(@NotNull Term a, Term b, TermIndex i) {
        //TODO construct TermSet directly
        return TermContainer.difference(
                a.op(),
            (Compound) a, (Compound) b
        );
    }
}
