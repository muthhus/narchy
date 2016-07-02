package nars.nal.op;

import nars.Op;
import nars.term.atom.AtomicStringConstant;


public abstract class TermTransformOperator extends AtomicStringConstant implements TermTransform {

    private final String id;

    public TermTransformOperator() {
        this.id = "^" + getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public Op op() {
        return Op.OPER;
    }
}
