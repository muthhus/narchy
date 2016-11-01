package nars.op;

import nars.NAR;
import nars.Op;
import nars.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.transform.TermTransformOperator;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static nars.$.varDep;
import static nars.$.varIndep;
import static nars.Op.*;

/**
 * 1-iteration DepVar and IndepVar introduction that emulates and expands the original NAL6 Variable Introduction Rules
 */
public class DepIndepVarIntroduction extends VarIntroduction {



    public DepIndepVarIntroduction(Random rng) {
        super(1, rng);
    }

//    @Override
//    public void accept(@NotNull Task task) {
//
//        if (task.cyclic())
//            return; //avoids reprocessing the same
//
//        super.accept(task);
//    }



    final static int ConjOrStatementBits = Op.IMPL.bit | Op.EQUI.bit | Op.CONJ.bit;
    final static int DepOrIndepBits = Op.VAR_INDEP.bit | Op.VAR_DEP.bit | Op.VAR_PATTERN.bit;
    static final Predicate<Term> condition = subterm -> !subterm.isAny(DepOrIndepBits);

    @Nullable
    @Override
    protected Term[] select(Compound input) {
        return Terms.substAllRepeats(input, condition, 2);
    }

    @Nullable
    @Override
    protected Term[] next(@NotNull Compound input, Term selected) {

        if (selected == Imdex)
            return null;

        List<byte[]> p = input.pathsTo(selected);
        if (p.isEmpty())
            return null;

        //detect an invalid top-level indep var substitution
        if (input.op().statement) {
            for (byte[] path : p)
                if (path.length < 2)
                    return null; //substitution would replace something at the top level of a statement
        }


        int pSize = p.size();
        ObjectByteHashMap<Term> conjCoverage = new ObjectByteHashMap<>(pSize);
        ObjectByteHashMap<Term> indepEquivCoverage = new ObjectByteHashMap<>(pSize /* estimate */);
        for (int occurrence = 0; occurrence < pSize; occurrence++) {
            byte[] path = p.get(occurrence);
            Term t = null; //root
            int pathLength = path.length;
            for (int i = -1; i < pathLength-1 /* dont include the selected term itself */; i++) {
                if (i == -1)
                    t = input;
                else
                    t = ((Compound) t).term(path[i]);
                Op o = t.op();
                if (validIndepVarSuperterm(o)) {
                    byte inside = (byte) (1 << path[i + 1]);
                    indepEquivCoverage.updateValue(t, inside, (previous) -> (byte) ((previous) | inside));
                } else if (validDepVarSuperterm(o)) {
                    conjCoverage.addToValue(t, (byte) 1);
                }
            }
        }

        int iteration = 0;

        //at least one impl/equiv must have both sides covered
        Term I = (indepEquivCoverage.anySatisfy(b -> b == 0b11)) ?
                varIndep("i" + iteration) : null;

        //at least one conjunction must contain >=2 path instances
        Term D = conjCoverage.anySatisfy(b -> b >= 2) ?
                varDep("i" + iteration) : null;

        if (I!=null && D!=null) {
            return new Term[] { I , D };
        } else if (I!=null) {
            return new Term[] { I };
        } else if (D!=null) {
            return new Term[] { D };
        } else {
            return null;
        }

    }

    private static boolean validDepVarSuperterm(Op o) {
        return /*o.statement ||*/ o == CONJ;
    }

    static boolean validIndepVarSuperterm(Op o) {
        return o == IMPL || o == EQUI;
    }

    public static final class VarIntro extends TermTransformOperator {

        @NotNull
        final DepIndepVarIntroduction introducer;

        public VarIntro(@NotNull NAR nar) {
            super("varIntro");
            this.introducer = new DepIndepVarIntroduction(nar.random);
        }

        @Override
        public @NotNull Term apply(@NotNull Term[] args) {

            Term[] only = new Term[] { False };
            introducer.accept((Compound) args[0], y -> only[0] = y);
            return only[0];
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

