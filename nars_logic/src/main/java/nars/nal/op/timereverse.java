//package nars.nal.op;
//
//import nars.nal.nal7.Tense;
//import nars.term.Term;
//import nars.term.TermContainer;
//import nars.term.compile.TermBuilder;
//import nars.term.compound.Compound;
//
//public class timereverse extends UnaryTermOperator/*implements BinaryOperator<Term>*/ {
//
//    @Override
//    public Term apply(Term a, TermBuilder i) {
//        //TODO construct TermSet directly
//        if (a instanceof Compound) {
//            Compound c = (Compound)a;
//            int t = c.t();
//            if (t!= Tense.ITERNAL) {
//                return c.t(-t);
//            }
//        }
//        return a;
//    }
// }
