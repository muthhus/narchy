package nars.op.sys.scheme.expressions;

import org.jetbrains.annotations.NotNull;

public class NumberExpression implements Expression {
    public final long value;

    public NumberExpression(long value) {
        this.value = value;
    }

    @NotNull
    public static NumberExpression number(long n) {
        return new NumberExpression(n);
    }

    @Override
    public boolean equals(@NotNull Object o) {
        return getClass() == o.getClass() && value == ((NumberExpression) o).value;
    }

    @Override
    public int hashCode() {
        return (int) (value ^ (value >>> 32));
    }

    public String toString() {
        return String.format("number(%s)", value);
    }

    @NotNull
    @Override
    public String print() {
        return Long.toString(value);
    }
}
