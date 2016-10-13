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


public interface TermTransform extends Atomic {

    Concept[] StaticBuiltins = {
            new intersect(),
            new differ(),
            new union()
    };

    @NotNull Term function(@NotNull Compound args);



    @NotNull
    @Override
    default Op op() {
        return Op.ATOM;
    }

}
