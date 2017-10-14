package nars.term.atom;

import com.google.common.collect.Range;
import nars.index.term.TermContext;
import nars.term.Term;

public interface Intlike extends Atomic {

    Range range();

//    @Override
//    default Term eval(TermContext context) {
//        return this;
//    }
//
//    @Override
//    default Term evalSafe(TermContext context, int remain) {
//        return this;
//    }

}
