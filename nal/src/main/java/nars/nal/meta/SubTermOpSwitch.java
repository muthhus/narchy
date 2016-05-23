package nars.nal.meta;

import nars.nal.meta.op.SubTermOp;
import nars.term.atom.Atom;

import java.util.Map;

/**
 * Created by me on 5/21/16.
 */
public final class SubTermOpSwitch extends Atom /* TODO represent as some GenericCompound */ implements ProcTerm {

    final ProcTerm[] proc = new ProcTerm[32]; //should be large enough
    public final int subterm;


    public SubTermOpSwitch(int subterm, Map<SubTermOp, ProcTerm> cases) {
        super("\"" + cases.toString() + "\"");

        this.subterm = subterm;

        cases.forEach((c,p) -> {
            proc[c.op] = p;
        });
    }

    @Override
    public void accept(PremiseEval m) {
        ProcTerm p = proc[m.subOp(subterm)];
        if (p!=null) {
            p.accept(m);
        }
    }
}
