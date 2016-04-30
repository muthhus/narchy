package nars.nal.meta.constraint;

import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.subst.FindSubst;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;

import static nars.term.container.TermContainer.commonSubterms;
import static nars.term.container.TermContainer.subtermIsCommon;


public final class NoCommonSubtermsConstraint implements MatchConstraint {

    @NotNull
    private final Term b;

    public NoCommonSubtermsConstraint(@NotNull Term b) {
        this.b = b;
    }

    @Override
    public boolean invalid(@NotNull Term x, @NotNull Term y, @NotNull FindSubst f) {
        Term B = f.term(b);

        if (B == null)
            return false;

        //variables excluded, along with 'nonVarSubtermIsCommon' predicate in the compound vs compound case
        if ((B instanceof Variable) || (y instanceof Variable))
            return false;

        if (y instanceof Compound) {

            Compound C = (Compound) y;
            if (B instanceof Compound) {
                return commonSubterms((Compound) B, C,
                        subtermIsCommon //variables are excluded by the
                        //nonVarSubtermIsCommon
                );
            } else {
                return C.containsTermRecursively(B);
            }

        } else if (B instanceof Compound) {
            return ((Compound)B).containsTermRecursively(y);
        }

        return B.equals(y);
    }

    @NotNull
    @Override
    public String toString() {
        return "noCommonSubterms(" + b + ')';
    }

//    public static boolean commonSubterms(Term a, Term b, Set<Term> s) {
//        addUnmatchedSubterms(a, s, null);
//        return !addUnmatchedSubterms(b, null, s); //we stop early this way (efficiency)
//    }
//
//    private static boolean addUnmatchedSubterms(Term x, @Nullable Set<Term> AX, @Nullable Set<Term> BX) {
//        if (BX != null && BX.contains(x)) { //by this we can stop early
//            return false;
//        }
//
//        if (AX != null && AX.add(x) && x instanceof Compound) {
//            Compound c = (Compound) x;
//            int l = c.size();
//            for (int i = 0; i < l; i++) {
//                Term d = c.term(i);
//                if (!addUnmatchedSubterms(d, AX, BX)) {
//                    //by this we can stop early
//                    return false;
//                }
//            }
//        }
//
//        return true;
//    }



}
