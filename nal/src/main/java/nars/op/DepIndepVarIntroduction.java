package nars.op;

import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.index.term.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.container.TermContainer;
import nars.term.Functor;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.ToIntFunction;

import static nars.Op.*;

/**
 * 1-iteration DepVar and IndepVar introduction that emulates and expands the original NAL6 Variable Introduction Rules
 */
public class DepIndepVarIntroduction extends VarIntroduction {


    final static int ConjOrStatementBits = Op.IMPL.bit | Op.EQUI.bit | Op.CONJ.bit; //NOT including similarity or inheritance because variables acorss these would be loopy

    private final static int DepOrIndepBits = Op.VAR_INDEP.bit | Op.VAR_DEP.bit | Op.VAR_PATTERN.bit;

    /** sum by complexity if passes include filter */
    private static final ToIntFunction<Term> depIndepFilter = t ->
            t.hasAny(DepOrIndepBits | Op.NEG.bit) ? 0 : 1;
    private final NAR nar;

    public DepIndepVarIntroduction(NAR nar) {
        this.nar = nar;
    }

    @Override
    protected TermIndex index() {
        return nar.terms;
    }

    //(t.op()==VAR_INDEP || t.op()==VAR_DEP) ? 0 : 1;

    @Nullable
    @Override
    protected FasterList<Term> select(Compound input) {
        return Terms.substAllRepeats(input, depIndepFilter, 2);
    }

    @Nullable
    @Override
    protected Term next(@NotNull Compound input, @NotNull Term selected, int order) {

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
            case EQUI:
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
                t = (i == -1) ? input : ((Compound) t).sub(p[i]);
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
                    $.v(VAR_INDEP, "x" + order) /*varIndep(order)*/ : null;

        } else {
            //at least one conjunction must contain >=2 path instances
            return m.anySatisfy(b -> b >= 2) ?
                    $.v(VAR_DEP, "x" + order)  /* $.varDep(order) */ : null;
        }

    }

    public static boolean validDepVarSuperterm(Op o) {
        return /*o.statement ||*/ o == CONJ;
    }

    public static boolean validIndepVarSuperterm(Op o) {
        return o.statement;
        //return o == IMPL || o == EQUI;
    }

    public static final class VarIntro extends Functor {

        @NotNull
        final DepIndepVarIntroduction introducer;

        public VarIntro(NAR nar) {
            super("varIntro");
            this.introducer = new DepIndepVarIntroduction(nar);
        }



        @Override
        public @NotNull Term apply(@NotNull TermContainer args) {
            return introduce(args.sub(0));
        }

        @NotNull
        public Term introduce(Term x) {
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

