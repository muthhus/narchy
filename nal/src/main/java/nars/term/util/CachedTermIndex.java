package nars.term.util;

import jcog.bag.impl.hijack.HijackMemoize;
import nars.$;
import nars.Op;
import nars.Param;
import nars.index.term.AppendProtoCompound;
import nars.index.term.ProtoCompound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.Null;

/**
 * memoizes term construction, in attempt to intern as much as possible (but not exhaustively)
 */
public class CachedTermIndex extends StaticTermIndex {

    final static Logger logger = LoggerFactory.getLogger(nars.term.util.CachedTermIndex.class);

    public static final HijackMemoize<ProtoCompound, Term> terms =
            termMemoize(256 * 1024, 3, Float.NaN, Float.NaN);

    public static final StaticTermIndex _terms = new StaticTermIndex();

    public static HijackMemoize<ProtoCompound, Term> termMemoize(int capacity, int reprobes, float boost, float cut) {
        HijackMemoize<ProtoCompound, Term> h = new HijackMemoize<>( capacity, reprobes,
                (C) -> {
                    try {
                        return _terms.the(C.op(), C.dt(), C.subterms());
                    } catch (InvalidTermException e) {
                        if (Param.DEBUG_EXTRA)
                            logger.error("{}", e);
                        return Null;
                    } catch (Throwable t) {
                        logger.error("{}", t);
                        return Null;
                    }
                }
        );

        if (boost==boost && cut==cut)
            h.set(boost, cut);

        return h;
    }


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
