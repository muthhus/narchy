package nars.term.variable;

import nars.$;
import nars.Op;
import org.jetbrains.annotations.NotNull;

public final class CommonVariable extends Variable  {


    CommonVariable(Op type, int a, int b) {
        super(type, a << 8 + b); //this limits # of variables to 256 per term
    }


    @Override
    public final int vars() {
        // pattern variable hidden in the count 0
        return type == Op.VAR_PATTERN ? 0 : 1;
    }

    @Override
    public final Op op() {
        return type;
    }


    @Override
    public final int varIndep() {
        return type == Op.VAR_INDEP ? 1 : 0;
    }

    @Override
    public final int varDep() {
        return type == Op.VAR_DEP ? 1 : 0;
    }

    @Override
    public final int varQuery() {
        return type == Op.VAR_QUERY ? 1 : 0;
    }

    @Override
    public int varPattern() {
        return type == Op.VAR_QUERY ? 1 : 0;
    }

    @NotNull
    public static CommonVariable make(@NotNull Variable v1, @NotNull Variable v2) {


//        if (v1 instanceof CommonVariable) {
//            return (CommonVariable)v1; //combine into common common variable
//            //System.out.println(v1 + " " + v2);
//        }
//        if (v2 instanceof CommonVariable) {
//            return (CommonVariable)v2; //combine into common common variable
//            //System.out.println(v1 + " " + v2);
//        }

        Op type = v1.op();
        if (v2.op()!=type)
            throw new RuntimeException("differing types");

        int a = v1.id;
        int b = v2.id;

        int cmp = Integer.compare(a, b);

        //lexical ordering: swap
        if (cmp > 0) {
            int t = a;
            a = b;
            b = t;
        }
        else if (cmp == 0) {
            throw new RuntimeException("variables equal");
        }

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
