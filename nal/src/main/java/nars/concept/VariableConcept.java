//package nars.concept;
//
//import nars.bag.Bag;
//import nars.term.variable.Variable;
//import org.jetbrains.annotations.NotNull;
//
///**
// *
// */
//public final class VariableConcept extends AtomConcept<Variable> implements Variable {
//
//    public VariableConcept(@NotNull Variable atom) {
//        super(atom, Bag.EMPTY, Bag.EMPTY);
//    }
//
//    @Override
//    public int vars() {
//        return 1;
//    }
//
//    @Override
//    public int varIndep() {
//        return term.varIndep();
//    }
//
//    @Override
//    public int varDep() {
//        return term.varDep();
//    }
//
//    @Override
//    public int varQuery() {
//        return term.varQuery();
//    }
//
//    @Override
//    public int varPattern() {
//        return term.varPattern();
//    }
//
//
//    @Override
//    public final int id() {
//        return term.id();
//    }
//}
