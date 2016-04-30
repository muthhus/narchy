//package nars.nal.op;
//
//import nars.term.Compound;
//import nars.term.Term;
//import nars.term.TermIndex;
//import org.jetbrains.annotations.NotNull;
//
///**
// * Created by me on 12/12/15.
// */
//public abstract class UnaryTermOperator extends ImmediateTermTransform {
//
//    @Override public final Term function(@NotNull Compound x, TermIndex i) {
//        if (x.size()<1)
//            throw new RuntimeException(this + " requires >= 2 args");
//
//        return apply(x.term(0), i);
//    }
//
//    @NotNull
//    public abstract Term apply(Term a, TermIndex i);
//}
