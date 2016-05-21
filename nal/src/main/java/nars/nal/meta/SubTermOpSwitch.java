package nars.nal.meta;

import nars.Op;
import nars.nal.meta.op.SubTermOp;
import nars.term.Compound;
import nars.term.SubtermVisitor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.AtomicString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Created by me on 5/21/16.
 */
public final class SubTermOpSwitch extends Atom /* TODO represent as some GenericCompound */ implements ProcTerm {

    final ProcTerm[] proc = new ProcTerm[16]; //should be large enough
    private final int subterm;


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
