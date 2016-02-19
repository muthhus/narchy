package nars.op.software.scheme.expressions;


import nars.term.atom.Atom;
import org.jetbrains.annotations.NotNull;

public class SymbolExpression extends Atom implements Expression {



    public SymbolExpression(String value) {
        super(value);
    }

    @NotNull
    public static SymbolExpression symbol(String s) {
        return new SymbolExpression(s);
    }


    @Override
    public String print() {
        return toString();
    }

}
