package nars.nal.meta;

import nars.Op;
import nars.term.compound.GenericCompound;
import nars.term.container.TermVector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**

 < (&&, cond1, cond2, ...) ==> (&|, fork1, fork2, ... ) >
 < (&&, cond1, cond2, ...) ==> end >
 */
public final class PremiseBranch extends GenericCompound implements ProcTerm {

//    public static class PremiseIf extends GenericCompound implements ProcTerm {
//        public PremiseIf(@NotNull BooleanCondition<PremiseEval> cond, ProcTerm conseq) {
//            super(Op.IMPLICATION,
//                    TermVector.the(cond, conseq));
//        }
//        @Override public void accept(@NotNull PremiseEval m) {
//            int r = m.now();
//            if (cond.booleanValueOf(m)) {
//                conseq.accept(m);
//            }
//            m.revert(r);
//        }
//    }

    @NotNull
    public final transient BooleanCondition<PremiseEval> cond;
    @NotNull
    public final transient ProcTerm conseq;


//    @Override
//    public void appendJavaProcedure(@NotNull StringBuilder s) {
//        s.append("if (");
//        cond.appendJavaCondition(s);
//        s.append(") {\n");
//        s.append("\t ");
//        conseq.appendJavaProcedure(s);
//        s.append("\n}");
//    }


    public PremiseBranch(@NotNull List<BooleanCondition<PremiseEval>> cond, ProcTerm conseq) {
        super(Op.IMPLICATION,
            TermVector.the(
                        cond.size() == 1 ? cond.get(0) : new AndCondition(cond),
                        conseq)
        );

        this.cond = (BooleanCondition<PremiseEval>) term(0);
        this.conseq = (ProcTerm) term(1);
    }

    @Override public void accept(@NotNull PremiseEval m) {
        int r = m.now();
        if (cond.booleanValueOf(m)) {
            conseq.accept(m);
        }
        m.revert(r);
    }


}
