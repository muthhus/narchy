//package nars.op.java;
//
//import nars.$;
//import nars.index.term.TermIndex;
//import nars.nal.nal8.operator.TermFunction;
//import nars.term.Compound;
//import nars.term.Term;
//import nars.term.atom.Atom;
//import org.jetbrains.annotations.NotNull;
//
///**
// * resolve a java class from String to its usage knowledge
// */
//public class jclass extends TermFunction {
//
//    final Termizer termizer = new DefaultTermizer();
//
//    @Override public Object function(@NotNull Compound x, TermIndex i) {
//
//        try {
//            Term t = x.term(0);
//            if (t instanceof Atom) {
//                String tt = $.unquote(t);
//                Class c = Class.forName(tt);
//                return $.p(termizer.term(c), termizer.term(c.getMethods()));
//            }
//            return null;
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//
//    }
//}
