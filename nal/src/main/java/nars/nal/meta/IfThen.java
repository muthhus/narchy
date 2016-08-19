package nars.nal.meta;

import nars.Op;
import nars.term.Term;
import nars.term.compound.GenericCompound;
import nars.term.container.TermVector;
import org.jetbrains.annotations.NotNull;

/**

 < (&&, cond1, cond2, ...) ==> (&|, fork1, fork2, ... ) >
 < (&&, cond1, cond2, ...) ==> end >
 */
public final class IfThen extends GenericCompound<Term> implements ProcTerm {


    public final transient @NotNull BoolCondition cond;
    @NotNull
    public final transient ProcTerm conseq;


    public IfThen(@NotNull BoolCondition cond, @NotNull ProcTerm conseq) {
        super(Op.IMPL,
            TermVector.the( cond, conseq)
        );

        this.cond = cond; //(BoolCondition) term(0);
        this.conseq = conseq; //(ProcTerm) term(1);
    }

    @Override public void accept(@NotNull PremiseEval m, int now) {
        if (cond.booleanValueOf(m)) {
            conseq.accept(m, m.now() /* since evaluating the bool will have added to the stack */);
        }
        m.revert(now);
    }


//    @Override
//    public void appendJavaProcedure(@NotNull StringBuilder s) {
//        s.append("if (");
//        cond.appendJavaCondition(s);
//        s.append(") {\n");
//        s.append("\t ");
//        conseq.appendJavaProcedure(s);
//        s.append("\n}");
//    }
}
