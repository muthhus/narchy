package nars.index;

import nars.$;
import nars.bag.Bag;
import nars.concept.AtomConcept;
import nars.concept.PermanentConcept;
import nars.op.data.differ;
import nars.op.data.intersect;
import nars.op.data.union;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.TermTransform;
import org.jetbrains.annotations.NotNull;

/**
 * static-level instant term transform operations
 */
public final class TermTransformConcept extends AtomConcept implements PermanentConcept, TermTransform {

    public static final TermTransformConcept[] BuiltIn = {
            new TermTransformConcept(new intersect()),
            new TermTransformConcept(new differ()),
            new TermTransformConcept(new union())
    };

    @NotNull
    private final TermTransform function;

    private TermTransformConcept(@NotNull String opName, @NotNull TermTransform o) {
        super($.the(opName), Bag.EMPTY, Bag.EMPTY);
        this.function = o;
    }

    private TermTransformConcept(@NotNull TermTransform o) {
        this(o.getClass().getSimpleName(), o);
    }

    @NotNull
    @Override
    public final Term function(@NotNull Compound args) {
//            if (args.varPattern() > 0) {
//                //return the operation which would have been constructed for the pattern
//                return index.b(Op.INH, args, this);
//            }
        return function.function(args);
    }
}
