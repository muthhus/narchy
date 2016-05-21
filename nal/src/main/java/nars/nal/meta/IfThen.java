package nars.nal.meta;

import nars.Op;
import nars.term.compound.GenericCompound;
import nars.term.container.TermVector;
import org.jetbrains.annotations.NotNull;

/**

 < (&&, cond1, cond2, ...) ==> (&|, fork1, fork2, ... ) >
 < (&&, cond1, cond2, ...) ==> end >
 */
public final class IfThen extends GenericCompound implements ProcTerm {


    public final transient @NotNull BoolCondition cond;
    @NotNull
    public final transient ProcTerm conseq;


    public IfThen(@NotNull BoolCondition cond, ProcTerm conseq) {
        super(Op.IMPLICATION,
            TermVector.the( cond, conseq)
        );

        this.cond = (BoolCondition) term(0);
        this.conseq = (ProcTerm) term(1);
    }

    @Override public void accept(@NotNull PremiseEval m) {
        final int stack = m.now();
        if (cond.booleanValueOf(m)) {
            conseq.accept(m);
        }
        m.revert(stack);
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
