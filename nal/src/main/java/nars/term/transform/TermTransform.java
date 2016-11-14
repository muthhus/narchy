package nars.term.transform;

import nars.Op;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;


public interface TermTransform extends Atomic, Function<Term[],Term> {



    @NotNull
    @Override
    default Op op() {
        return Op.ATOM;
    }

}
