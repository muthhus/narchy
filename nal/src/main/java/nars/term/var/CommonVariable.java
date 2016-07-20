package nars.term.var;

import nars.Op;
import org.jetbrains.annotations.NotNull;

public final class CommonVariable extends GenericNormalizedVariable {



    CommonVariable(@NotNull Op type, int a, int b) {
        super(type, multiVariable(a, b));
    }


    public static @NotNull GenericNormalizedVariable make(@NotNull Variable v1, @NotNull Variable v2) {


//        if (v1 instanceof CommonVariable) {
//            return (CommonVariable)v1; //combine into common common variable
//            //System.out.println(v1 + " " + v2);
//        }
//        if (v2 instanceof CommonVariable) {
//            return (CommonVariable)v2; //combine into common common variable
//            //System.out.println(v1 + " " + v2);
//        }

        int a = v1.id();
        int b = v2.id();
        if (a == b) {
            //throw new RuntimeException("variables equal");
        }

        //lexical ordering: swap
        if (b > a) {
            int t = a;
            a = b;
            b = t;
        }

        Op type = v1.op();
        if (v2.op()!=type)
            throw new RuntimeException("differing types");

        return new CommonVariable(type, a, b);
    }

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
