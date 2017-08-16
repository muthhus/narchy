package nars.term.var;

import nars.$;
import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

public final class CommonVariable extends GenericNormalizedVariable {


    CommonVariable(@NotNull Op type, int hash) {
        super(type, hash);
    }

    @Override
    public int opX() {
        return Term.opX(op(), 1 /* different from normalized variables with a subOp of 0 */);
    }


    public static @NotNull Variable common(@NotNull AbstractVariable A, @NotNull AbstractVariable B) {

        int ai = A.id;        assert(ai < (1 << 7));
        Op Aop = A.op();
        byte ao = Aop.id;
        int bi = B.id;        assert(bi < (1 << 7));
        byte bo = B.op().id;

        int h = (ao << 24) | (ai << 16) | (bi << 8) | bo;

        return new CommonVariable(Aop, h);

    }

//    public boolean common(@NotNull AbstractVariable y) {
//        int yid = y.id;
//
//        int v1 = (hash & 0xff) - 1; //unhash
//        if (v1 == yid)
//            return true;
//
//        int v2 = ((hash >> 8) & 0xff) - 1; //unhash
//        return v2 == yid;
//
//    }


//    //TODO use a 2d array not an enum map, just flatten the 4 op types to 0,1,2,3
//    /** variables x 10 (digits) x (1..10) (digits) cache;
//     *  triangular matrix because the pairs is sorted */
//    static final EnumMap<Op,CommonVariable[][]> common = new EnumMap(Op.class);
//    static {
//        for (Op o : new Op[] { Op.VAR_PATTERN, Op.VAR_QUERY, Op.VAR_INDEP, Op.VAR_DEP}) {
//            CommonVariable[][] cm = new CommonVariable[10][];
//            for (int i = 0; i < 10; i++) {
//                cm[i] = new CommonVariable[i+2];
//            }
//            common.put(o, cm);
//        }
//    }


}
