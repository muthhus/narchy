package nars.nal.op;

import nars.Op;
import nars.term.atom.AtomicStringConstant;
import org.jetbrains.annotations.NotNull;


public abstract class TermTransformOperator extends AtomicStringConstant implements TermTransform {

    @NotNull
    private final String id;

    public TermTransformOperator() {
        this.id = "^" + getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return id;
    }

    @NotNull
    @Override
    public Op op() {
        return Op.OPER;
    }
}
