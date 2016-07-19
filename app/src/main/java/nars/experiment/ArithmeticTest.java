package nars.experiment;

import com.gs.collections.api.list.primitive.ByteList;
import com.gs.collections.impl.list.mutable.primitive.IntArrayList;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.Op;
import nars.nar.Default;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.util.Texts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static nars.Op.CONJ;
import static nars.Op.INH;
import static nars.Op.PROD;

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

        new ArtithmeticInduction1(n);

        Global.DEBUG = true;
        n.log();
        n.input("((x,1) && (x,2)).");
        n.input("((x,2) && (x,3)).");

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
        if (term instanceof Atomic) {
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
    public static class ArtithmeticInduction1 implements Consumer<Task> {
        private final NAR n;

        static final Term vv =
                //$.$("#relating");
                $.$("$relating");

        public ArtithmeticInduction1(NAR n) {
            this.n = n;
            n.onTask(this);
        }

        @Override
        public void accept(Task b) {

            if (b.isBeliefOrGoal() && b.op() == CONJ) {

                Compound<?> bt = b.term();
                TermContainer<?> subs = bt.subterms();
                int negs = subs.count(x -> x.op() == Op.NEG);

                boolean negate = (negs == bt.size());
                if (negs != 0 && !negate) {
                    //only if none or all of the subterms are negated
                    return;
                }

                if (!subs.equivalentStructures())
                    return;

                if (negate) {
                    subs = $.neg((TermVector)subs);
                }

                //paths * extracted sequence of numbers at given path for each subterm
                Map<ByteList,IntArrayList> numbers = new HashMap();

                //first subterm: infer location of all inductables
                BiPredicate<ByteList, @Nullable Integer> collect = (p, t) -> {
                    IntArrayList c = numbers.computeIfAbsent(p, (pp) -> new IntArrayList(1));
                    c.add(t);
                    return false; //just the first
                };

                Compound<?> first = (Compound)subs.term(0);
                first.pathsTo(ArithmeticTest::intOrNull, collect);
                int paths = numbers.size();
                if (paths != 1)
                    return; //HACK for now, just handle case where there is a uniform structure shared by all subterms

                //analyze remaining subterms
                for (int i = 1; i < subs.size(); i++) {
                    subs.term(i).pathsTo(ArithmeticTest::intOrNull, collect);
                    if (numbers.size() != paths)
                        return;  //inconsistent with the first term
                }



                Map.Entry<ByteList, IntArrayList> ppnn = numbers.entrySet().iterator().next();
                List<Term> features = features(ppnn.getValue(), vv);

                Term pattern = $.terms.transform(first, ppnn.getKey(), vv);
                features.add(pattern);

                Compound result = (Compound) $.compound(CONJ, bt.dt(), features);

                n.inputLater(
                        task(b, result).log(getClass().getSimpleName())
                );


            }


        }

        private List<Term> features(IntArrayList numbers, Term relatingVar) {
            List<Term> ll = Global.newArrayList();
            ll.add(iRange(numbers, relatingVar));

            //...

            return ll;
        }


        public Term iRange(IntArrayList l, Term relatingVariable) {
            int min = l.min();
            int max = l.max();
            //return $.$("l1Dist(" + $.varDep(1) + ", " + (Math.abs(x - y)) + ")");
            return $.p($.p($.the("intRange"), relatingVariable), $.the(min), $.the(max-min));
        }


        @NotNull MutableTask task(Task b, Term tt) {
            return new MutableTask(
                    tt,
                    b.punc(), b.truth())
                    .time(n.time(), b.occurrence())
                    .budget(b)
                    .evidence(b.evidence());
        }
    }

    static class NumericDifferenceRuleOld implements Consumer<Task> {
        private final NAR n;

        public NumericDifferenceRuleOld(NAR n) {
            this.n = n;
            n.onTask(this);
        }

        @Override
        public void accept(Task b) {

            if (b.isBeliefOrGoal() && b.op() == CONJ) {

                Compound<?> bt = b.term();
                TermContainer<?> subs = bt.subterms();
                int negs = subs.count(x -> x.op() == Op.NEG);

                boolean negate = (negs == bt.size());
                if (negs != 0 && !negate) {
                    //only if none or all of the subterms are negated
                    return;
                }

                if (negate) {
                    subs = $.neg((TermVector)subs);
                }

                //type of subterms, must all be the same
                Op subType = null;

                if (subType == null) {
                    int ins = subs.count(x -> x.op() == INH);
                    if (ins == bt.size())
                        subType = INH;
                }
                if (subType == null) {
                    int ins = subs.count(x -> x.op() == PROD);
                    if (ins == bt.size())
                        subType = PROD;
                }

                if (subType == null)
                    return;

                Set<Term> subjs = subs.unique(x -> ((Compound) x).term(0));
                Set<Term> preds = subs.unique(x -> ((Compound) x).term(1));

                //use a generic variable so that it wont interfere with any existing
                Term vv =
                        //$.$("#relating");
                        $.$("$relating");
                //$.varDep(1000);
                //$.varIndep(1000);

                if (preds.size() == 1) {
                    Term pp = preds.iterator().next();
                    if (pp.op()!=vv.op() && subjs.size() == 2) {
                        Term[] ss = subjs.toArray(new Term[subjs.size()]);
                        Integer x = intOrNull(ss[0]);
                        Integer y = intOrNull(ss[1]);
                        if (x != null && y != null) {
                            add(b, subType,
                                    vv, pp, negate,
                                    integerRelations(vv, x, y));
                        }
                    }

                } else if (subjs.size() == 1) {
                    Term ss = subjs.iterator().next();
                    if (ss.op()!=vv.op() && preds.size() == 2) {
                        Term[] pp = preds.toArray(new Term[preds.size()]);
                        Integer x = intOrNull(pp[0]);
                        Integer y = intOrNull(pp[1]);
                        if (x != null && y != null) {
                            add(b, subType,
                                    ss, vv, negate,
                                    integerRelations(vv, x, y));
                        }
                    }

                }

            }
        }

        public @NotNull Term[] integerRelations(Term vv, Integer x, Integer y) {
            return new Term[] {
                    //l1Dist(vv, x, y),
                    iDelta(vv, x, y)
            };
        }

        public @NotNull Term l1Dist(Term relatingVariable, Integer x, Integer y) {
            //return $.$("l1Dist(" + $.varDep(1) + ", " + (Math.abs(x - y)) + ")");
            return $.p($.p($.the("l1Dist"), relatingVariable), $.the(Math.abs(x - y)));
        }
        public @NotNull Term iDelta(Term relatingVariable, Integer x, Integer y) {
            int min = Math.min(x, y);
            //return $.$("l1Dist(" + $.varDep(1) + ", " + (Math.abs(x - y)) + ")");
            return $.p($.p($.the("iDelta"), relatingVariable), $.the(min), $.the(Math.abs(x - y)));
        }

        public void add(Task b, Op type,  Term newSubj, Term newPred, boolean negate, Term... rules) {
            Term pattern = $.compound(type, newSubj, newPred);

            for (Term rule : rules) {
                n.inputLater(
                        task(b,
                                $.conj(
                                        $.negIf(pattern, negate),
                                        b.dt(),
                                        rule
                                )
                        ).log("l1Dist")
                );
            }
        }

        @NotNull MutableTask task(Task b, Term tt) {
            return new MutableTask(
                    tt,
                    b.punc(), b.truth())
                    .time(n.time(), b.occurrence())
                    .budget(b)
                    .evidence(b.evidence());
        }
    }

}
