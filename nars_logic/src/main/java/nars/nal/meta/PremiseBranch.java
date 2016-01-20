package nars.nal.meta;

import nars.Op;
import nars.term.TermVector;
import nars.term.compound.GenericCompound;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**

 < (&&, cond1, cond2, ...) ==> (&|, fork1, fork2, ... ) >
 < (&&, cond1, cond2, ...) ==> end >
 */
public final class PremiseBranch extends GenericCompound implements ProcTerm<PremiseMatch> {

    @NotNull
    public final transient AndCondition<PremiseMatch> cond;
    @NotNull
    public final transient ProcTerm<PremiseMatch> conseq;


    @Override
    public void appendJavaProcedure(@NotNull StringBuilder s) {
        s.append("if (");
        cond.appendJavaCondition(s);
        s.append(") {\n");
        s.append("\t ");
        conseq.appendJavaProcedure(s);
        s.append("\n}");
    }

    public PremiseBranch(@NotNull Collection<BooleanCondition<PremiseMatch>> cond, ProcTerm<PremiseMatch> conseq) {
        super(Op.IMPLICATION,
                new TermVector(new AndCondition(cond), conseq));
        this.cond = (AndCondition<PremiseMatch>) term(0);
        this.conseq = (ProcTerm<PremiseMatch>) term(1);
    }

    @Override public void accept(PremiseMatch m) {
        int r = m.now();
        if (cond.booleanValueOf(m)) {
            conseq.accept(m);
        }
        m.revert(r);
    }


}
