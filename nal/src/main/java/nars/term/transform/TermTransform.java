package nars.term.transform;

import nars.Op;
import nars.concept.Concept;
import nars.op.data.differ;
import nars.op.data.intersect;
import nars.op.data.union;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;


public interface TermTransform extends Atomic, Function<Term[],Term> {

    Concept[] StaticBuiltins = {
            new intersect(),
            new differ(),
            new union()
    };



    @NotNull
    @Override
    default Op op() {
        return Op.ATOM;
    }

}
