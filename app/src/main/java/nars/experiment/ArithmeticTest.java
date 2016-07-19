package nars.experiment;

import nars.$;
import nars.Global;
import nars.Op;
import nars.index.TransformConcept;
import nars.nar.Default;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.util.Texts;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by me on 7/18/16.
 */
public class ArithmeticTest {
    public static void main(String[] args) {
        Default n = new Default();
        n.on(new TransformConcept("ADD", (c) -> {
            Term X = c.term(0);
            Term Y;
            if (c.size() < 2)
                Y = $.the("NaN");
            else
                Y = c.term(1);

            if (X.op()==Op.SETe) {
                final int[] sum = {0};
                if (!X.and(x -> {
                    Integer a = intOrNull(x);
                    if (a == null)
                        return false;
                    else {
                        sum[0] += a;
                        return true;
                    }
                }))
                    Y = $.the("NaN");
                else
                    Y = $.the(sum[0]);
            }

            return $.inh($.p(X, Y), $.oper("ADD"));
        }));
        n.onTask(q -> {
            if (q.isQuestion() && q.op()== Op.SIM) {
                Term a = q.term(0);
                Term b = q.term(1);
                if (a.op()==Op.SETe && b.op() == Op.SETe) {
                    Compound A = (Compound)a;
                    Compound B = (Compound)b;
                    int bs = B.size();
                    int as = A.size();
                    int max = Math.max(as, bs);
                    int plus = as + bs;
                    Set<Term> e = new HashSet(plus);
                    Collections.addAll(e, A.terms());
                    Collections.addAll(e, B.terms());
                    float uniques = plus - e.size();
                    float similarity = ( uniques/max);
                    if (similarity>0) //remain silent about cases where nothing is common
                        n.inputLater(new MutableTask($.sim(a,b),'.',similarity,n).log("StructuralSimilarity"));
                }
            }
        });
        Global.DEBUG = true;
        n.log();
        n.input("numbers:{2,3,4}.");
        n.input("(&&, numbers:#x, ADD(#x)).");
        n.input("ADD(#x,7)?");
        //n.input("numbers:{1}.");
        //n.input("numbers:{3}.");
        //n.input("numbers:{1,3}.");
        ///n.input("numbers:{1,3}?");
        n.run(1024);

    }

    @Nullable
    public static Integer intOrNull(Term term) {
        if (term instanceof Atom) {
            return Texts.i(term.toString());
        }
        if (term instanceof Compound && term.op() == Op.SETe) {
            Compound c = ((Compound)term);
            if (c.size() == 1) {
                return intOrNull(c.term(0)); //unwrap singleton extset
            }
        }
        return null;
    }
}
