package nars.term.var;

import com.google.common.base.Joiner;
import nars.Op;
import nars.term.Term;
import org.eclipse.collections.api.set.primitive.ByteSet;
import org.eclipse.collections.api.set.primitive.ImmutableByteSet;
import org.eclipse.collections.impl.factory.primitive.ByteSets;
import org.jetbrains.annotations.Nullable;

public final class CommonVariable extends UnnormalizedVariable {

    public final ImmutableByteSet vars;

    CommonVariable(/*@NotNull*/ Op type, byte a, byte b) {
        this(type, ByteSets.immutable.of(a, b));
        assert(a!=b);
    }

    CommonVariable(/*@NotNull*/ Op type, ImmutableByteSet vars) {
        super(type, Joiner.on('_').join(vars.collect(b -> Integer.toString(b, 36))) );
        this.vars = vars;
    }

    @Override
    public int opX() {
        return Term.opX(op(), 1 /* different from normalized variables with a subOp of 0 */);
    }

    public static Variable common(Variable A, Variable B) {

        Op Aop = A.op();
        assert(B.op()==Aop);

        boolean aa = A instanceof AbstractVariable;
        boolean bb = B instanceof AbstractVariable;
        if (aa && bb) {
            byte ai = ((AbstractVariable)A).id();
            byte bi = ((AbstractVariable)B).id();
            return new CommonVariable(Aop, ai, (byte) -bi);
        }

        if (!aa && bb) {
            ImmutableByteSet ai = ((CommonVariable)A).vars;
            byte bi = ((AbstractVariable)B).id();
            if (ai.contains((byte) -bi))
                return A;
            return new CommonVariable(Aop, ai.newWith((byte) -bi));
        }

        if (aa && !bb) {
            byte ai = ((AbstractVariable)A).id();
            ImmutableByteSet bi = ((CommonVariable)B).vars;
            if (bi.contains((byte) ai))
                return B;
            return new CommonVariable(Aop, bi.newWith(ai));
        }

        /*if (!aa && !bb)*/ {
            ImmutableByteSet ai = ((CommonVariable)A).vars;
            ImmutableByteSet bi = ((CommonVariable)B).vars;
            ImmutableByteSet combined = ai.newWithAll(bi);
            if (combined.equals(ai))
                return A;
            return new CommonVariable(Aop, combined);
        }

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
