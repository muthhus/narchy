package nars.experiment;

import nars.$;
import nars.Global;
import nars.NAR;
import nars.Op;
import nars.index.TransformConcept;
import nars.nar.Default;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.util.Texts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by me on 7/18/16.
 */
public class ArithmeticTest {
    public static void main(String[] args) {
        Default n = new Default();
//        n.on(new TransformConcept("INT", (c) -> {
//            if (c.size()!=1) return null;
//            Term p = c.term(0);
//            Term y;
//            if (p.op().var) {
//                y = p;
//            } else {
//                Integer x = intOrNull(p);
//                if (x == null)
//                    return null;
//                y = p;
//            }
//            return $.inh($.p(y), $.oper("INT"));
//        }));

//        n.on(new TransformConcept("ADD", (c) -> {
//            Term X = c.term(0);
//            Term Y;
//            if (c.size() < 2)
//                Y = $.the("NaN");
//            else
//                Y = c.term(1);
//
//            if (X.op()==Op.SETe) {
//                final int[] sum = {0};
//                if (!X.and(x -> {
//                    Integer a = intOrNull(x);
//                    if (a == null)
//                        return false;
//                    else {
//                        sum[0] += a;
//                        return true;
//                    }
//                }))
//                    Y = $.the("NaN");
//                else
//                    Y = $.the(sum[0]);
//            }
//
//            return $.inh($.p(X, Y), $.oper("ADD"));
//        }));
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

        new NumericDifferenceRule(n);

        Global.DEBUG = true;
        n.log();
        n.input("(x:1 && x:2).");
        n.input("(x:2 && x:3).");

//        n.input("numbers:{1,2,3,4}.");
//        n.input("numbers:{1,2,3,4}.");
//        //n.input("(numbers:{#x} && INT(#x)).");
//
//        n.input("(&&, numbers:{#x}, numbers:{#y}).",
//                "((&&, numbers:{#x}, numbers:{#y}) ==> INTS(#x,#y)).");
//        //n.input("((&&, INT(#x), INT(#y)) ==> INTS(#x,#y)).");
////        n.input("(&&, numbers:#x, ADD(#x)).");
////        n.input("ADD(#x,7)?");
////        //n.input("numbers:{1}.");
////        //n.input("numbers:{3}.");
////        //n.input("numbers:{1,3}.");
////        ///n.input("numbers:{1,3}?");
        n.run(1024);

    }

    @Nullable
    public static Integer intOrNull(Term term) {
        if (term instanceof Atom) {
            int i = Texts.i(term.toString(), Integer.MIN_VALUE);
            if (i == Integer.MIN_VALUE)
                return null;
            return i;
        }
        if (term instanceof Compound && term.op() == Op.SETe) {
            Compound c = ((Compound)term);
            if (c.size() == 1) {
                return intOrNull(c.term(0)); //unwrap singleton extset
            }
        }
        return null;
    }

    public static class NumericDifferenceRule implements Consumer<Task> {
        private final NAR n;

        public NumericDifferenceRule(NAR n) {
            this.n = n;
            n.onTask(this);
        }

        @Override
        public void accept(Task b) {
        /* arithmetic rule mining
            ex:
                (&&, (x:1), (x:2) )
                -->
                (&&, (x:#y), (diff(#y,1)) )
        */
            if (b.isBelief() && b.op() == Op.CONJ) {

                Compound<?> bt = b.term();
                int negs = bt.subterms().count(x -> x.op() == Op.NEG);

                if (negs != 0/* && negs!= bt.size()*/) {
                    //only if none or all of the subterms are negated
                    return;
                }

                //only handle all inhs
                //int ins = bt.subterms().count(x -> x.op() == Op.INH);
                int ins = bt.subterms().count(x -> x.op() == Op.PROD);
                if (ins != bt.size())
                    return;

                Set<Term> preds = bt.unique(x -> ((Compound) x).term(1));
                if (preds.size() == 1) {

                    Term pp = preds.iterator().next();

                    if (pp.op() == Op.VAR_DEP) //prevent loop
                        return;

                    Set<Term> subjs = bt.unique(x -> ((Compound) x).term(0));
                    if (subjs.size() == 2) {
                        Term[] ss = subjs.toArray(new Term[subjs.size()]);
                        Integer x = intOrNull(ss[0]);
                        Integer y = intOrNull(ss[1]);
                        if (x != null && y != null) {
                            int d = Math.abs(x - y);
                            @Nullable Compound pattern = $.inh($.varDep(1), pp);
                            @NotNull Term rule = $.$("diff(" + $.varDep(1) + ", " + d + ")");
                            n.inputLater(new MutableTask(
                                    $.conj(
                                            pattern,
                                            b.dt(),
                                            rule
                                    ),
                                    '.', b.expectation(), n)
                                    .time(n.time(), b.occurrence())
                                    .evidence(b.evidence())
                                    .log("Difference"));
                        }
                    }
                }

            }
        }
    }
}
