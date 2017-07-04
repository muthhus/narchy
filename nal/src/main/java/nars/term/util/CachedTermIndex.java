package nars.term.util;

import jcog.util.CaffeineMemoize;
import jcog.util.Memoize;
import nars.Op;
import nars.Param;
import nars.index.term.AppendProtoCompound;
import nars.index.term.ProtoCompound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import static nars.Op.Null;

/**
 * memoizes term construction, in attempt to intern as much as possible (but not exhaustively)
 */
public class CachedTermIndex extends StaticTermIndex {

    final static Logger logger = LoggerFactory.getLogger(nars.term.util.CachedTermIndex.class);

//    public static final StaticTermIndex _terms = new StaticTermIndex();

    @Override
    public @NotNull Term the(@NotNull Op op, int dt, @NotNull Term... u) {
        if (u.length < 2)
            return super.the(op, dt, u);


        //return terms.apply(new AppendProtoCompound(op, dt, u).commit());
        return _the(op, dt, u);
    }

    Term _the(@NotNull Op op, int dt, @NotNull Term[] u) {
        return op.the(dt, u);
//        return ((Term)terms.apply(
//                new AppendProtoCompound(op, u).commit(dt)
//        )).dt(dt);
    }


//    @Override
//    public Term the(ProtoCompound mustBeCommitedPriorToCall) {
//        if (mustBeCommitedPriorToCall.size() < 2)
//            return super.the(mustBeCommitedPriorToCall);
//
////        if (!c.isDynamic()) {
////            build.miss.increment();
////            return super.the(c.op(), c.dt(), c.subterms()); //immediate construct
////        } else {
//
//
////        int cdt = c.dt();
////        if (cdt !=DTERNAL && c.op().temporal && concurrent(cdt)) {
////            //special case: construct using the common DTERNAL form and then wrap via GenericCompoundDT
////            return terms.apply(c.dt(DTERNAL).commit()).dt(cdt);
////        }
//
//        return (Term) terms.apply(mustBeCommitedPriorToCall);
////        }
//    }

}
