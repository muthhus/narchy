package nars.derive.meta;

import nars.term.Term;

import java.util.function.Predicate;

/** a term representing a native predicate */
public interface PrediTerm<X> extends Term, Predicate<X> {


}
