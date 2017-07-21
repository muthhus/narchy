//package nars.derive.meta.op;
//
//import nars.derive.meta.BoolPredicate;
//import TaskBeliefSubterms;
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Arrays;
//import java.util.function.BiPredicate;
//
///**
// * Created by me on 12/17/15.
// */
//public final class TermNotEquals extends SubtermPathCondition {
//
//    private final BiPredicate<Term,Term> test;
//
//    /** TODO the shorter path should be set for 'a' if possible, because it will be compared first */
//    protected TermNotEquals(int a, byte[] aPath, int b, byte[] bPath, BiPredicate<Term,Term> test) {
//        super(aPath, a, bPath, b, test.toString());
//        this.test = test;
//    }
//
//
//
////    @NotNull
////    public static BoolPredicate the(@NotNull TaskBeliefSubterms p, BiPredicate<Term,Term> equality) {
////        if (p.a < p.b) {
////            return new TermNotEquals(p.a, p.aPath, p.b, p.bPath, equality);
////        } else if (p.a > p.b) {
////            return new TermNotEquals(p.b, p.bPath, p.a, p.aPath, equality);
////        } else {
////            //sort by the path
////            int pc = Arrays.compare(p.aPath, p.bPath);
////            if (pc < 0) {
////                return new TermNotEquals(p.a, p.aPath, p.b, p.bPath, equality);
////            } else if (pc > 0) {
////                return new TermNotEquals(p.b, p.bPath, p.a, p.aPath, equality);
////            } else {
////                throw new UnsupportedOperationException();
////            }
////        }
////    }
//
//
//    @Override
//    protected boolean eval(@NotNull Term a, Term b) {
//        return !test.test(a,b);
//    }
//
//}
