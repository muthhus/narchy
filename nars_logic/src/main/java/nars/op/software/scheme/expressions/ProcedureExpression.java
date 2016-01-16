package nars.op.software.scheme.expressions;


import nars.op.software.scheme.cons.Cons;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class ProcedureExpression implements Expression, Function<Cons<Expression>, Expression> {

    final Function<Cons<Expression>, Expression> lambda;

    protected ProcedureExpression(Function<Cons<Expression>, Expression> lambda) {
        this.lambda = lambda;
    }

    @NotNull
    public static ProcedureExpression procedure(Cons<SymbolExpression> names, Cons<Expression> exps, Function<Cons<Expression>, Expression> lambda) {
        return new SymbolicProcedureExpression(names, exps, lambda);
    }
    @NotNull
    public static ProcedureExpression procedure(Function<Cons<Expression>, Expression> lambda) {
        return new ProcedureExpression(lambda);
    }

    @Override
    public boolean equals(@NotNull Object o) {
        return getClass() == o.getClass() && lambda.equals(((ProcedureExpression) o).lambda);
    }

    @Override
    public int hashCode() {
        return lambda.hashCode();
    }


    @NotNull
    @Override
    public String print() {
        return "Procedure(" + lambda + ')';
    }

    @NotNull
    @Override
    public String toString() {
        return print();
    }


    @Override
    public Expression apply(Cons<Expression> expressions) {
        return lambda.apply(expressions);
    }

}
