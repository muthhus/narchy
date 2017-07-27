package nars.op.data;

import nars.term.Functor;
import nars.term.Term;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class union extends Functor.BinaryFunctor {

    public union() {
        super("union");
    }

    @Nullable
    @Override public Term apply(@NotNull Term a, @NotNull Term b) {
        return Terms.union(a.op(), a.subterms(), b.subterms() );
    }

}
