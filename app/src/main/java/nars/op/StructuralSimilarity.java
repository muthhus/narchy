package nars.op;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by me on 7/20/16.
 */
public class StructuralSimilarity implements Consumer<Task> {
    private final NAR n;

    public StructuralSimilarity(NAR n) {
        this.n = n;
    }

    @Override
    public void accept(Task q) {
        if (q.isQuestion() && q.op() == Op.SIM) {
            Term a = q.term(0);
            Term b = q.term(1);
            if (a.op() == Op.SETe && b.op() == Op.SETe) {
                Compound A = (Compound) a;
                Compound B = (Compound) b;
                int bs = B.size();
                int as = A.size();
                int max = Math.max(as, bs);
                int plus = as + bs;
                Set<Term> e = new HashSet(plus);
                Collections.addAll(e, A.terms());
                Collections.addAll(e, B.terms());
                float uniques = plus - e.size();
                float similarity = (uniques / max);
                if (similarity > 0) //remain silent about cases where nothing is common
                    n.inputLater(new MutableTask($.sim(a, b), '.', similarity, n).log("StructuralSimilarity"));
            }
        }
    }

}
