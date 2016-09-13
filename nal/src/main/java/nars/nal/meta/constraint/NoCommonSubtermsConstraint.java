package nars.nal.meta.constraint;

import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.FindSubst;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

import static nars.term.container.TermContainer.commonSubtermsRecurse;
import static nars.term.container.TermContainer.subtermOfTheOther;

/** variables excluded */
public final class NoCommonSubtermsConstraint implements MatchConstraint {

    @NotNull
    private final Term b;

    public NoCommonSubtermsConstraint(@NotNull Term b) {
        this.b = b;
    }


    @Override
    public boolean invalid(@NotNull Term x, @NotNull Term y, @NotNull FindSubst f) {
        if (y instanceof Variable)
            return false;

        Term B = f.xy(b);

        if (B == null || B instanceof Variable)
            return false;

        boolean bCompound = B instanceof Compound;

        if (y instanceof Compound) {

            Compound C = (Compound) y;

            return bCompound ?
                    subtermOfTheOther((Compound)B, C, true)
                    //commonSubtermsRecurse((Compound) B, C, true, new HashSet())
                    //commonSubterms((Compound) B, C, true, scratch.get())
                    :
                    C.containsTerm(B);

        } else {

            return bCompound ?
                    B.containsTerm(y)
                    :
                    B.equals(y);
        }

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
