package nars.op.software.scheme.expressions;

import org.jetbrains.annotations.NotNull;

public class StringExpression implements Expression {
    public final String value;

    public StringExpression(String value) {
        this.value = value;
    }

    @NotNull
    public static StringExpression string(String s) {
        return new StringExpression(s);
    }

    @Override
    public boolean equals(@NotNull Object o) {
        return getClass() == o.getClass() && value.equals(((StringExpression) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public String toString() {
        return String.format("string(\"%s\")", value);
    }

    @Override
    public String print() {
        return value;
    }
}
