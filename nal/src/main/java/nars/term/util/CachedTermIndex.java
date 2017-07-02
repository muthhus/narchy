package nars.term.util;

import jcog.bag.impl.hijack.HijackMemoize;
import jcog.util.CaffeineMemoize;
import jcog.util.Memoize;
import nars.Op;
import nars.Param;
import nars.index.term.AppendProtoCompound;
import nars.index.term.ProtoCompound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import static nars.Op.Null;
import static nars.Op.concurrent;
import static nars.time.Tense.DTERNAL;

/**
 * memoizes term construction, in attempt to intern as much as possible (but not exhaustively)
 */
public class CachedTermIndex extends StaticTermIndex {

    final static Logger logger = LoggerFactory.getLogger(nars.term.util.CachedTermIndex.class);

    public static final StaticTermIndex _terms = new StaticTermIndex();
    static final Function<ProtoCompound, Term> buildTerm = (C) -> {
            try {
                return _terms.the(C.op(), C.dt(), C.subterms());
            } catch (InvalidTermException e) {
                if (Param.DEBUG_EXTRA)
                    logger.error("Term Build: {}, {}", C, e);
                return Null;
            } catch (Throwable t) {
                logger.error("{}", t);
                return Null;
            }
        };

    public static final Memoize<ProtoCompound, Term> terms =
            new HijackMemoize<>(buildTerm, 384 * 1024, 4);
            //CaffeineMemoize.build(buildTerm, 384 * 1024,  true /* Param.DEBUG*/ );


    @Override
    public @NotNull Term the(@NotNull Op op, int dt, @NotNull Term... u) {
//        if (u.length < 2)
//            return super.the(op, dt, u);


        //return terms.apply(new AppendProtoCompound(op, dt, u).commit());
        return the(new AppendProtoCompound(op, dt, u));
    }

    @Override
    public Term the(ProtoCompound c) {
//        if (c.size() < 2)
//            return super.the(c);

//        if (!c.isDynamic()) {
//            build.miss.increment();
//            return super.the(c.op(), c.dt(), c.subterms()); //immediate construct
//        } else {


//        int cdt = c.dt();
//        if (cdt !=DTERNAL && c.op().temporal && concurrent(cdt)) {
//            //special case: construct using the common DTERNAL form and then wrap via GenericCompoundDT
//            return terms.apply(c.dt(DTERNAL).commit()).dt(cdt);
//        }

        return terms.apply(c.commit());
//        }
    }

}
