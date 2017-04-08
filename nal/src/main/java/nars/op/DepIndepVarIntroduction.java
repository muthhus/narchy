package nars.op;

import nars.$;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.transform.Functor;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import static nars.Op.*;

/**
 * 1-iteration DepVar and IndepVar introduction that emulates and expands the original NAL6 Variable Introduction Rules
 */
public class DepIndepVarIntroduction extends VarIntroduction {


    final static int ConjOrStatementBits = Op.IMPL.bit | Op.EQUI.bit | Op.CONJ.bit;
    private final static int DepOrIndepBits = Op.VAR_INDEP.bit | Op.VAR_DEP.bit | Op.VAR_PATTERN.bit;

    /** sum by complexity if passes include filter */
    private static final ToIntFunction<Term> depIndepScore = t ->
            t.hasAny(DepOrIndepBits | Op.NEG.bit) ? 0 : 1;
            //(t.op()==VAR_INDEP || t.op()==VAR_DEP) ? 0 : 1;

    @Nullable
    @Override
    protected MutableSet<Term> select(Compound input) {
        return Terms.substAllRepeats(input, depIndepScore, 2);
    }

    @Nullable
    @Override
    protected Term next(@NotNull Compound input, @NotNull Term selected, int order) {

        if (selected == Imdex)
            return null;

        List<byte[]> p = input.pathsTo(selected);
        int pSize = p.size();
        if (pSize == 0)
            return null;

        //detect an invalid top-level indep var substitution
        Op inOp = input.op();
        if (inOp.statement) {
            for (int i = 0; i < pSize; i++) {
                if (p.get(i).length < 2)
                    return null; //substitution would replace something at the top level of a statement}
            }
        }

        //decide what kind of variable can be introduced according to the input operator
        boolean depOrIndep;
        switch (inOp) {
            case CONJ:
                depOrIndep = true;
                break;
            case IMPL:
            case EQUI:
                depOrIndep = false;
                break;
            default:
                return null; //????
                //throw new UnsupportedOperationException();
        }


        @Nullable ObjectByteHashMap<Term> m = new ObjectByteHashMap<>(pSize);
        for (int occurrence = 0; occurrence < pSize; occurrence++) {
            byte[] path = p.get(occurrence);
            Term t = null; //root
            int pathLength = path.length;
            for (int i = -1; i < pathLength-1 /* dont include the selected term itself */; i++) {
                t = (i == -1) ? input : ((Compound) t).term(path[i]);
                Op o = t.op();

                if (!depOrIndep && validIndepVarSuperterm(o)) {
                    byte inside = (byte) (1 << path[i + 1]);
                    m.updateValue(t, inside, (previous) -> (byte) ((previous) | inside));
                } else if (depOrIndep && validDepVarSuperterm(o)) {
                    m.addToValue(t, (byte) 1);
                }
            }
        }


        if (!depOrIndep) {
            //at least one impl/equiv must have both sides covered
            return (m.anySatisfy(b -> b == 0b11)) ?
                    $.v(VAR_INDEP, "x" + order) /*varIndep(order)*/ : null;

        } else {
            //at least one conjunction must contain >=2 path instances
            return m.anySatisfy(b -> b >= 2) ?
                    $.v(VAR_DEP, "x" + order)  /* $.varDep(order) */ : null;
        }

    }

    private static boolean validDepVarSuperterm(Op o) {
        return /*o.statement ||*/ o == CONJ;
    }

    private static boolean validIndepVarSuperterm(Op o) {
        return o == IMPL || o == EQUI;
    }

    public static final class VarIntro extends Functor {

        @NotNull
        final DepIndepVarIntroduction introducer;

        public VarIntro() {
            super("varIntro");
            this.introducer = new DepIndepVarIntroduction();
        }

        @Override
        public @NotNull Term apply(@NotNull Term... args) {
            return introduce(args[0]);
        }

        @NotNull
        protected Term introduce(Term x) {
            Term[] only = { False };

            //temporarily unwrap negation
            boolean negated = x.op() == NEG;
            Term xx = negated ? x.unneg() : x;

            if (!(xx instanceof Compound))
                return x;

            introducer.accept((Compound)xx, y -> only[0] = y);

            Term o = only[0];
            return (o == False) ? False : $.negIf(o, negated);
        }
    }

}

//        protected boolean introduce(Term x) {
//            return true;
////            return x instanceof Compound &&
////                   introducer.rng.nextFloat() <=
////                            1f / (1 + x.size())
////                           //1f / Math.sqrt(x.volume())
////                           //1f / x.volume()
////                           //0.5f;
////                           //(1f / (1f + x.size()))
////            ;
//        }

