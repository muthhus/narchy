package nars.nal.meta;

import nars.Op;
import nars.term.compound.GenericCompound;
import nars.term.container.TermVector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**

 < (&&, cond1, cond2, ...) ==> (&|, fork1, fork2, ... ) >
 < (&&, cond1, cond2, ...) ==> end >
 */
public final class PremiseBranch extends GenericCompound implements ProcTerm {

    @NotNull
    public final transient AndCondition<PremiseEval> cond;
    @NotNull
    public final transient ProcTerm conseq;


    @Override
    public void appendJavaProcedure(@NotNull StringBuilder s) {
        s.append("if (");
        cond.appendJavaCondition(s);
        s.append(") {\n");
        s.append("\t ");
        conseq.appendJavaProcedure(s);
        s.append("\n}");
    }

    public PremiseBranch(@NotNull Collection<BooleanCondition<PremiseEval>> cond, ProcTerm conseq) {
        super(Op.IMPLICATION,
                new TermVector(new AndCondition(cond), conseq));
        this.cond = (AndCondition<PremiseEval>) term(0);
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
