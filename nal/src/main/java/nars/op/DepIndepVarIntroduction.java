package nars.op;

import nars.$;
import nars.NAR;
import nars.Op;
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

        //decide what kind of variable can be introduced according to the input operator
        boolean dep, indep;
        if (input.op() == CONJ) {
            dep = true; indep = false;
        } else if (input.isAny(ImplicationOrEquivalenceBits)) {
            dep = false; indep = true;
        } else {
            throw new UnsupportedOperationException();
        }


        int pSize = p.size();
        @Nullable ObjectByteHashMap<Term> conjCoverage = dep ? new ObjectByteHashMap<>(pSize) : null;
        @Nullable ObjectByteHashMap<Term> indepEquivCoverage = indep ? new ObjectByteHashMap<>(pSize /* estimate */) : null;
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

                if (indep && validIndepVarSuperterm(o)) {
                    byte inside = (byte) (1 << path[i + 1]);
                    indepEquivCoverage.updateValue(t, inside, (previous) -> (byte) ((previous) | inside));
                } else if (dep && validDepVarSuperterm(o)) {
                    conjCoverage.addToValue(t, (byte) 1);
                }
            }
        }

        int iteration = 0;

        //at least one impl/equiv must have both sides covered
        Term I = indep && (indepEquivCoverage.anySatisfy(b -> b == 0b11)) ?
                varIndep("i" + iteration) : null;

        //at least one conjunction must contain >=2 path instances
        Term D = dep && conjCoverage.anySatisfy(b -> b >= 2) ?
                varDep("i" + iteration) : null;

        /*if (I!=null && D!=null) {
            return new Term[] { I , D };
        } else */if (I!=null) {
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

            Term[] only = { False };
            Term x = args[0];

            //temporarily unwrap negation
            Term xx;
            boolean negated;
            if (x.op() == NEG) {
                xx = x.unneg();
                negated = true;
            } else {
                xx = x;
                negated = false;
            }

            if (!(x instanceof Compound))
                return x;

            x = xx;
            introducer.accept((Compound)x, y -> only[0] = y);

            Term y = only[0];
            if (y == False)
                return False;
            return $.negIf(y, negated);
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

