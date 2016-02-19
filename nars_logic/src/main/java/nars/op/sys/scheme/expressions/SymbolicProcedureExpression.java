package nars.op.sys.scheme.expressions;

import nars.op.sys.scheme.cons.Cons;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Attaches input symbol code to the procedure so it may be accessed later
 */
public class SymbolicProcedureExpression extends ProcedureExpression {
    public final Cons<Expression> exps;
    public final Cons<SymbolExpression> names;

    public SymbolicProcedureExpression(Cons<SymbolExpression> names,
                                       Cons<Expression> exps, Function<Cons<Expression>, Expression> lambda) {
        super(lambda);
        this.names = names;
        this.exps = exps;
    }

    @NotNull
    @Override
    public String toString() {
        return names + "=" + exps;
    }
}
