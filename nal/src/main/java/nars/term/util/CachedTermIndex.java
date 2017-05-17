package nars.term.util;

import jcog.bag.impl.hijack.HijackMemoize;
import nars.Op;
import nars.Param;
import nars.index.term.AppendProtoCompound;
import nars.index.term.ProtoCompound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.False;

/**
 * memoizes term construction, in attempt to intern as much as possible (but not exhaustively)
 */
public class CachedTermIndex extends StaticTermIndex {

    final static Logger logger = LoggerFactory.getLogger(nars.term.util.CachedTermIndex.class);

    protected final HijackMemoize<ProtoCompound, Term> terms = new HijackMemoize<>(
            256 * 1024, 3,
            (C) -> {
                try {
                    return super.the(C.op(), C.dt(), C.subterms());
                } catch (InvalidTermException e) {
                    if (Param.DEBUG_EXTRA)
                        logger.error("{}", e);
                    return False;
                } catch (Throwable t) {
                    logger.error("{}", t);
                    return False;
                }
            }
    );


//        @Override
//        public float value(@NotNull ProtoCompound protoCompound) {
//            //??
//        }
//    };

//    final HijackMemoize<Compound,Term> normalize = new HijackMemoize<>(
//        64 * 1024, 2,
//        super::normalize
//    );


    @Override
    public @NotNull Term the(@NotNull Op op, int dt, @NotNull Term... u) {
        if (u.length < 2)
            return super.the(op, dt, u);

        return terms.apply(new AppendProtoCompound(op, dt, u));
    }

    @Override
    public Term the(ProtoCompound c) {
        if (c.size() < 2)
            return super.the(c);

//        if (!c.isDynamic()) {
//            build.miss.increment();
//            return super.the(c.op(), c.dt(), c.subterms()); //immediate construct
//        } else {
        return terms.apply(c);
//        }
    }

}
