package nars.nal.meta.op;

import nars.Op;
import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import org.jetbrains.annotations.NotNull;

/**
 * a condition on the op of a pattern term (task=0, or belief=1)
 */
abstract public class AbstractPatternOp extends AtomicBoolCondition {

    public final int subterm;
    public final int op;

    @NotNull
    private final transient String id;


    public AbstractPatternOp(int subterm, @NotNull Op op) {
        this.subterm = subterm;
        this.op = op.ordinal();
        id = getClass().getSimpleName() + "(" + Integer.toString(subterm) + "&\"" + op + "\")";
    }

    public AbstractPatternOp(int subterm, int bits) {
        this.subterm = subterm;
        this.op = bits;
        id = getClass().getSimpleName() + "(" + Integer.toString(subterm) + "&" + bits + ")";
    }

    @NotNull
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean booleanValueOf(@NotNull PremiseEval ff) {
        return test(subterm == 0 ? ff.termSub0op : ff.termSub1op);
    }

    abstract public boolean test(int i);

    public static final class PatternOp extends AbstractPatternOp {

        public PatternOp(int subterm, @NotNull Op op) {
            super(subterm, op);
        }

        public PatternOp(int subterm, int structure) {
            super(subterm, structure);
        }


        @Override
        public boolean test(int i) {
            return i == op;
        }
    }

    public static final class PatternOpNot extends AbstractPatternOp {

        public PatternOpNot(int subterm, int structure) {
            super(subterm, structure);
        }

        @Override
        public boolean test(int i) {
            //the bit must not be set in the structure
            return (i & op) == 0;
        }
    }
}
