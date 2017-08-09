package nars.op;

import jcog.list.FasterList;
import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.Terms;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToIntFunction;

import static nars.Op.*;

/**
 * 1-iteration DepVar and IndepVar introduction that emulates and expands the original NAL6 Variable Introduction Rules
 */
public class DepIndepVarIntroduction extends VarIntroduction {

    static final VarIntro the = new VarIntro();

    @Nullable public static Pair<Term, Map<Term, Term>> varIntroX(Term x, Random rng) {
        return the.accept(x, rng);
    }

    @Nullable public static Term varIntro(Term x, Random rng) {
        Pair<Term, Map<Term, Term>> result = varIntroX(x, rng);
        if (result!=null) {
            return result.getOne();
        } else {
            return null;
        }
    }

    final static int ConjOrStatementBits = Op.IMPL.bit | Op.CONJ.bit; //NOT including similarity or inheritance because variables acorss these would be loopy

    private final static int DepOrIndepBits = Op.VAR_INDEP.bit | Op.VAR_DEP.bit | Op.VAR_PATTERN.bit;

    /** sum by complexity if passes include filter */
    private static final ToIntFunction<Term> depIndepFilter = t ->
            t.hasAny(DepOrIndepBits | Op.NEG.bit) ? 0 : 1;


    public DepIndepVarIntroduction() {

    }



    //(t.op()==VAR_INDEP || t.op()==VAR_DEP) ? 0 : 1;

    @Nullable
    @Override
    protected FasterList<Term> select(Term input) {
        return Terms.substAllRepeats(input, depIndepFilter, 2);
    }

    @Nullable
    @Override
    protected Term next(@NotNull Term input, @NotNull Term selected, int order) {

        if (selected.equals(Imdex))
            return null;

        Op inOp = input.op();
        List<byte[]> paths = input.pathsTo(selected, inOp.statement ? 2 : 0);
        int pSize = paths.size();
        if (pSize == 0)
            return null;

        //byte[][] paths = pp.toArray(new byte[pSize][]);

//        //detect an invalid top-level indep var substitution
//        if (inOp.statement) {
//            Iterator<byte[]> pp = paths.iterator();
//            while (pp.hasNext()) {
//                byte[] p = pp.next();
//                if (p.length < 2)
//                    pp.remove();;
//                for (byte[] r : paths)
//                    if (r.length < 2)
//                        return null; //substitution would replace something at the top level of a statement}
//            }
//        }


        //use the innermost common parent to decide the type of variable.
        //in case there is no other common parent besides input itself, use that.
        Term commonParent = input.commonParent(paths);
        Op commonParentOp = commonParent.op();

        //decide what kind of variable can be introduced according to the input operator
        boolean depOrIndep;
        switch (commonParentOp) {
            case CONJ:
                depOrIndep = true;
                break;
            case IMPL:
                depOrIndep = false;
                break;
            default:
                return null; //????
                //throw new UnsupportedOperationException();
        }


        @Nullable ObjectByteHashMap<Term> m = new ObjectByteHashMap<>(0);
        for (int occurrence = 0; occurrence < pSize; occurrence++) {
            byte[] p = paths.get(occurrence);
            Term t = null; //root
            int pathLength = p.length;
            for (int i = -1; i < pathLength-1 /* dont include the selected term itself */; i++) {
                t = (i == -1) ? input : t.sub(p[i]);
                Op o = t.op();

                if (!depOrIndep && validIndepVarSuperterm(o)) {
                    byte inside = (byte) (1 << p[i + 1]);
                    m.updateValue(t, inside, (previous) -> (byte) ((previous) | inside));
                } else if (depOrIndep && validDepVarSuperterm(o)) {
                    m.addToValue(t, (byte) 1);
                }
            }
        }


        if (!depOrIndep) {
            //at least one impl/equiv must have both sides covered
            return (m.anySatisfy(b -> b == 0b11)) ?
                    $.v(VAR_INDEP, order) /*varIndep(order)*/ : null;

        } else {
            //at least one conjunction must contain >=2 path instances
            return m.anySatisfy(b -> b >= 2) ?
                    $.v(VAR_DEP, order)  /* $.varDep(order) */ : null;
        }

    }

    public static boolean validDepVarSuperterm(Op o) {
        return /*o.statement ||*/ o == CONJ;
    }

    public static boolean validIndepVarSuperterm(Op o) {
        return o.statement;
        //return o == IMPL || o == EQUI;
    }

    public static final class VarIntro extends DepIndepVarIntroduction {

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

