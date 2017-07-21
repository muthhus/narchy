//package nars.nal.meta.op;
//
//import nars.nal.meta.AtomicBoolCondition;
//import nars.nal.meta.PremiseEval;
//import nars.nal.meta.TaskBeliefSubterms;
//import nars.term.Compound;
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//
///**
// * compound 'container' recursively contains 'contained' but also not equal to it
// */
//public final class ContainedBy extends SubtermPathCondition {
//
//    public ContainedBy(TaskBeliefSubterms x) {
//        super(x);
//    }
//
//    public boolean eval(Term container, Term contained) {
//        return container instanceof Compound && ((Compound) container).containsTermRecursively(contained);
//    }
//
//}
