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
import nars.term.variable.Variable;
import nars.util.Texts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static nars.Op.INH;
import static nars.Op.PROD;
import static nars.Op.VAR_DEP;
import static nars.nal.Tense.DTERNAL;

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

    /** arithmetic rule mining

                (&&, (x,1), (x,2) )
                    --> (&&, (x,#y), (diff(#y,1)) )
            TODO
                (&&, (x:1), (x:2) )
                    --> (&&, (x:#y), (diff(#y,1)) )

    */
    public static class NumericDifferenceRule implements Consumer<Task> {
        private final NAR n;

        public NumericDifferenceRule(NAR n) {
            this.n = n;
            n.onTask(this);
        }

        @Override
        public void accept(Task b) {

            if (b.isBelief() && b.op() == Op.CONJ) {

                Compound<?> bt = b.term();
                int negs = bt.subterms().count(x -> x.op() == Op.NEG);

                if (negs != 0/* && negs!= bt.size()*/) {
                    //only if none or all of the subterms are negated
                    return;
                }

                //only handle all inhs
                //int ins = bt.subterms().count(x -> x.op() == Op.INH);
                Op type = null;

                if (type == null) {
                    int ins = bt.subterms().count(x -> x.op() == INH);
                    if (ins == bt.size())
                        type = INH;
                }
                if (type == null) {
                    int ins = bt.subterms().count(x -> x.op() == PROD);
                    if (ins == bt.size())
                        type = PROD;
                }

                Set<Term> subjs = bt.unique(x -> ((Compound) x).term(0));
                Set<Term> preds = bt.unique(x -> ((Compound) x).term(1));

                //use a generic variable so that it wont interfere with any existing
                @NotNull Term vv = $.$("#related");
                if (preds.size() == 1) {
                    Term pp = preds.iterator().next();
                    if (pp.op()!=VAR_DEP && subjs.size() == 2) {
                        Term[] ss = subjs.toArray(new Term[subjs.size()]);
                        Integer x = intOrNull(ss[0]);
                        Integer y = intOrNull(ss[1]);
                        if (x != null && y != null) {
                            add(b, type,
                                    vv,  pp,
                                    l1Dist(vv, x, y));
                        }
                    }

                } else if (subjs.size() == 1) {
                    Term ss = subjs.iterator().next();
                    if (ss.op()!=VAR_DEP && preds.size() == 2) {
                        Term[] pp = preds.toArray(new Term[preds.size()]);
                        Integer x = intOrNull(pp[0]);
                        Integer y = intOrNull(pp[1]);
                        if (x != null && y != null) {
                            add(b, type,
                                    ss, vv,
                                    l1Dist(vv, x, y));
                        }
                    }

                }

            }
        }

        public @NotNull Term l1Dist(Term relatingVariable, Integer x, Integer y) {
            //return $.$("l1Dist(" + $.varDep(1) + ", " + (Math.abs(x - y)) + ")");
            return $.$("((l1Dist," + relatingVariable + "), " + (Math.abs(x - y)) + ")");
        }

        public void add(Task b, Op type, Term newSubj, Term newPred, Term... rules) {
            Term pattern = $.compound(type, newSubj, newPred);

            for (Term rule : rules) {
                n.inputLater(new MutableTask(
                        $.conj(
                                pattern,
                                b.dt(),
                                rule
                        ),
                        '.', b.expectation(), n)
                        .time(n.time(), b.occurrence())
                        .evidence(b.evidence())
                        .log("l1Dist"));
            }
        }
    }
}
