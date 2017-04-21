package nars.derive.meta;

import nars.term.Compound;
import nars.term.Term;

import java.util.function.Predicate;

/** a term referring to a predicate function */
public interface BoolPred<X> extends Term, Predicate<X> {


}
