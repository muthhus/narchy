//package nars.nal.meta.op;
//
//import nars.Op;
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//
///**
// * Created by me on 12/17/15.
// */
//public final class TermStructure extends MatchOp {
//    public final int bits;
//
//    public TermStructure(/*@NotNull*/ Op matchingType, int bits) {
//        this.bits = bits & (~matchingType.bit());
//    }
//
//    @Override
//    public boolean match(@NotNull Term t) {
//        int s = t.structure();
//        return (s | bits) == s;
//    }
//
//    @Override
//    public String toString() {
//        return /*"Struct = " + */ Integer.toString(bits, 2);
//    }
// }
