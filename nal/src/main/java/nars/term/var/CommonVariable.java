package nars.term.var;

import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

public final class CommonVariable extends GenericNormalizedVariable {


    CommonVariable(/*@NotNull*/ Op type, int hash) {
        super(type, hash);
    }

    @Override
    public int opX() {
        return Term.opX(op(), 1 /* different from normalized variables with a subOp of 0 */);
    }


    @Nullable
    public static Variable common(AbstractVariable A, AbstractVariable B) {

        Op Aop = A.op();
        assert(B.op()==Aop);

        if (A.compareTo(B) < 0) {
            //swap
            AbstractVariable C = A;
            A = B;
            B = C;
        }

        int ai = A.id();
        if (ai >= (1 << 7)) {
            //TODO support multi-common variables
            return null;
        }
        int bi = B.id();
        if (bi >= (1 << 7)) {
            //TODO support multi-common variables
            return null;
        }



        int h = (bi << 8) | ai;

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
