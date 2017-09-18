package nars.term.transform;

import nars.$;
import nars.index.term.TermContext;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.VAR_QUERY;

/** I = input term type, T = transformable subterm type */
@FunctionalInterface public interface CompoundTransform extends TermContext {


    @Override
    default @Nullable Termed apply(@NotNull Term t) {
        return null;
    }

    /**
     * change all query variables to dep vars
     */
    CompoundTransform queryToDepVar = (parent, subterm) -> {
        if (subterm.op() == VAR_QUERY) {
            return $.varDep((((Variable) subterm).id()));
        }
        return subterm;
    };

    @Nullable Term apply(@Nullable Compound parent, Term subterm);

    /** enable predicate determined by the superterm, tested before processing any subterms */
    default boolean testSuperTerm(@NotNull Compound c) {
        return true;
    }

//    CompoundTransform Identity = (parent, subterm) -> subterm;

//    CompoundTransform<Compound,Term> None = new CompoundTransform<Compound,Term>() {
//        @Override
//        public boolean test(Term o) {
//            return true;
//        }
//
//        @Nullable
//        @Override
//        public Term apply(Compound parent, Term subterm) {
//            return subterm;
//        }
//    };

}
