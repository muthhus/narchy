package nars.term.var;

import nars.$;
import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

public final class CommonVariable extends GenericNormalizedVariable {


    CommonVariable(@NotNull Op type, int a, int b) {
        super(type, hashMultiVar(a, b));
    }

    @Override public int opX() { return Term.opX(op(), 1 /* different from normalized variables with a subOp of 0 */);    }

    public static @NotNull Variable common(@NotNull Variable A, @NotNull Variable B) {


//        if (v1 instanceof CommonVariable) {
//            return (CommonVariable)v1; //combine into common common variable
//            //System.out.println(v1 + " " + v2);
//        }
//        if (v2 instanceof CommonVariable) {
//            return (CommonVariable)v2; //combine into common common variable
//            //System.out.println(v1 + " " + v2);
//        }


        if (A instanceof UnnormalizedVariable || B instanceof UnnormalizedVariable)
            throw new RuntimeException("Generic Variable being made Common");

        int a = A.id();
        int b = B.id();

        boolean aCommon = (A instanceof CommonVariable);
        boolean bCommon = (B instanceof CommonVariable);
        if (aCommon || bCommon) {

            if (aCommon && B instanceof AbstractVariable) {
                //check to see if b is included in a
                int a1 = multiVariable(a, true);
                int a2 = multiVariable(a, false);
                if ((a1 == b) || (a2 == b))
                    return A;

            } else if (bCommon && A instanceof AbstractVariable) {

                //check to see if a is included in b
                int b1 = multiVariable(b, true);
                int b2 = multiVariable(b, false);
                if ((b1 == a) || (b2 == a))
                    return B;

            }

            if (A.compareTo(B) < 0) {
                Variable v = A; //swap lexically
                A = B;
                B = v;
            }

            //one or both of these will not fit in a normalized variable,
            //so default to a String-based generic variable
            return $.v(A.op(),
                    A.toString().substring(1) /* remove leading variable char */  +
                            B.toString()
            );
            //throw new RuntimeException("variable oob");
        }

        assert(a!=b);

        //lexical ordering: swap
        if (b > a) {
            int t = a;
            a = b;
            b = t;
        }

        Op type = A.op();

        assert(B.op()==type);


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
