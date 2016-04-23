package nars.nal.meta.constraint;

import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

import static nars.term.container.TermContainer.*;


public final class NoCommonSubtermsConstraint implements MatchConstraint {

    @NotNull
    private final Term b;

    public NoCommonSubtermsConstraint(@NotNull Term b) {
        this.b = b;
    }

    @Override
    public boolean invalid(@NotNull Term x, @NotNull Term y, @NotNull FindSubst f) {
        if (y instanceof Compound) {
            Term B = f.term(b);
            return (B instanceof Compound) && //includes null test
                   commonSubterms((Compound) B, (Compound)y,
                           nonVarSubtermIsCommon);
        }
        return false;
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
