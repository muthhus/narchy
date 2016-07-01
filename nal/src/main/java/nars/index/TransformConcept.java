package nars.index;

import nars.$;
import nars.bag.Bag;
import nars.concept.AtomConcept;
import nars.nal.op.TermTransform;
import nars.op.data.differ;
import nars.op.data.intersect;
import nars.op.data.union;
import nars.term.Compound;
import nars.term.Term;

/**
 * static-level instant term transform operations
 */
public final class TransformConcept extends AtomConcept implements TermTransform {

    public static final TransformConcept[] BuiltIn = new TransformConcept[]{
            new TransformConcept(new intersect()),
            new TransformConcept(new differ()),
            new TransformConcept(new union())
    };

    private final TermTransform function;

    public TransformConcept(TermTransform o) {
        super($.operator(o.getClass().getSimpleName()), Bag.EMPTY, Bag.EMPTY);
        this.function = o;
    }

    @Override
    public Term function(Compound args) {
//            if (args.varPattern() > 0) {
//                //return the operation which would have been constructed for the pattern
//                return index.b(Op.INH, args, this);
//            }
        return function.function(args);
    }
}
