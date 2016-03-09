package nars.op.sys.scheme;


import nars.op.sys.scheme.exception.VariableNotDefinedException;
import nars.op.sys.scheme.expressions.Expression;
import nars.op.sys.scheme.expressions.SymbolExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class SchemeClosure {
    public final Map<SymbolExpression, Expression> bindings;
    public final SchemeClosure enclosingEnvironment;

    private SchemeClosure(Map<SymbolExpression, Expression> bindings, SchemeClosure enclosingEnvironment) {
        this.bindings = bindings;
        this.enclosingEnvironment = enclosingEnvironment;
    }

    public SchemeClosure(Map<SymbolExpression, Expression> bindings) {
        this(bindings, null);
    }

    public SchemeClosure() {
        this(new HashMap<>(nars.op.sys.scheme.DefaultEnvironment.PRIMITIVES));
    }

    @NotNull
    public SchemeClosure extend(Map<SymbolExpression, Expression> bindings) {
        return new SchemeClosure(bindings, this);
    }


    public Expression get(@NotNull SymbolExpression symbol) {
        SchemeClosure other = this;
        while (true) {
            Expression exists = other.bindings.get(symbol);
            if (exists!=null) {
                return exists;
            }
            if (other.enclosingEnvironment != null) {
                other = other.enclosingEnvironment;
                continue;
            }
            throw new VariableNotDefinedException(symbol.toString());
        }
    }

    public void set(@NotNull SymbolExpression symbol, Expression value) {
        if (bindings.containsKey(symbol)) {
            bindings.put(symbol, value);
        } else if (enclosingEnvironment != null) {
            enclosingEnvironment.set(symbol, value);
        } else {
            throw new VariableNotDefinedException(symbol.toString());
        }
    }

    public void define(SymbolExpression symbol, Expression value) {
        bindings.put(symbol, value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SchemeClosure that = (SchemeClosure) o;

        if (bindings != null ? !bindings.equals(that.bindings) : that.bindings != null) {
            return false;
        }
        return !(enclosingEnvironment != null ? !enclosingEnvironment.equals(that.enclosingEnvironment) : that.enclosingEnvironment != null);

    }

    @Override
    public int hashCode() {
        int result = bindings != null ? bindings.hashCode() : 0;
        result = 31 * result + (enclosingEnvironment != null ? enclosingEnvironment.hashCode() : 0);
        return result;
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + '@' + Integer.toUnsignedString( hashCode(), Character.MAX_RADIX);
    }

    public Stream<Expression> evalStream(@NotNull String input) {
        return eval(Reader.read(input));

    }

    public Stream<Expression> eval(@NotNull List<Expression> exprs) {
        return exprs.stream().map(e -> Evaluator.evaluate(e, this));
    }

    public List<Expression> eval(@NotNull String input) {
        return evalStream(input).collect(Collectors.toList());
    }


}
