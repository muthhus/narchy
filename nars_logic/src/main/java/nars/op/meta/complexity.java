package nars.op.meta;

import nars.nal.nal8.operator.TermFunction;
import nars.term.TermBuilder;
import nars.term.compound.Compound;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 3/6/15.
 */
public class complexity extends TermFunction<Integer> {

    @Override
    public Integer function(@NotNull Compound x, TermBuilder i) {
        return x.term(0).complexity();
    }
}
