package nars.term.transform;

import nars.$;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.Subst;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.VAR_QUERY;

/** I = input term type, T = transformable subterm type */
@FunctionalInterface public interface CompoundTransform  {

    CompoundTransform Identity = (parent, subterm) -> subterm;
    /**
     * change all query variables to dep vars
     */
    CompoundTransform queryToDepVar = (parent, subterm) -> {
        if (subterm.op() == VAR_QUERY) {
            return $.varDep((((Variable) subterm).id()));
        }
        return subterm;
    };

    @Nullable Term apply(@Nullable Compound parent, @NotNull Term subterm);

    /** enable predicate determined by the superterm, tested before processing any subterms */
    default boolean testSuperTerm(@NotNull Compound c) {
        return true;
    }

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
