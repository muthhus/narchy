//package nars.nal.meta.op;
//
//import nars.nal.meta.PremiseEval;
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
///**
// * Created by me on 8/15/15.
// */
//@Deprecated public class NotEqual extends PreCondition2 {
//
//    /** commutivity: sort the terms */
//    @NotNull
//    public static NotEqual make(@NotNull Term a, @NotNull Term b) {
//        return a.compareTo(b) <= 0 ? new NotEqual(a, b) : new NotEqual(b, a);
//    }
//
//    NotEqual(Term var1, Term var2) {
//        super(var1, var2);
//    }
//
//    @Override
//    public final boolean test(PremiseEval m, @Nullable Term a, @Nullable Term b) {
//        return (a != null) && (b != null) && !a.equals(b);
//    }
//
//}
