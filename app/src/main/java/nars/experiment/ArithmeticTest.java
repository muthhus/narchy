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
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.util.Texts;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static nars.Op.*;
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
        //n.onTask(new StructuralSimilarity(n));

        new ArithmeticTest.ArtithmeticInduction1(n);

        Global.DEBUG = true;
        n.log();

        n.input("((x,1) && (x,2)). :|:"); //should find one pattern

        //n.input("((x,1,2) && (x,2,4)). :|:"); //should find two patterns

        //n.input("((x,1,2) && (y,2,4)). :|:"); //should not apply because x!=y


        //n.input("((x,2,1) && (x,3,1)). :|:");

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
        n.run(128);

    }

    @Nullable
    public static IntArrayList intsOrNull(List<Term> term) {
        IntArrayList l = new IntArrayList(term.size());
        for (Term x : term) {
            Integer i = intOrNull(x);
            if (i == null)
                return null;
            l.add(i);
        }
        return l;
    }

    @Nullable
    public static Term intOrNullTerm(Term term) {
        if (term instanceof Atomic) {
            int i = Texts.i(term.toString(), Integer.MIN_VALUE);
            if (i == Integer.MIN_VALUE)
                return null;
            return term;
        }
        return null;
    }

    @Nullable
    public static Integer intOrNull(Term term) {
        if (term instanceof Atomic) {
            int i = Texts.i(term.toString(), Integer.MIN_VALUE);
            if (i == Integer.MIN_VALUE)
                return null;
            return i;
        }
//        if (term instanceof Compound && term.op() == Op.SETe) {
//            Compound c = ((Compound)term);
//            if (c.size() == 1) {
//                return intOrNull(c.term(0)); //unwrap singleton extset
//            }
//        }
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

            if (!b.isBeliefOrGoal()) {

            } else {
                if ( (b.op() == CONJ) && ((b.dt() == DTERNAL) || (b.dt() == 0))) {

                    compress(b, (features, pattern) -> {

                        for (Term f : features) {
                            n.inputLater(
                                    task(b, $.conj(f, pattern)).log(getClass().getSimpleName())
                            );
                            /*n.inputLater(
                                    (new MutableTask(
                                        $.sim(
                                        //$.equi(
                                            b.term(), $.conj(f, b.dt(), pattern)
                                        ),
                                    '.', 1f, n).log(getClass().getSimpleName()))
                            );*/
                        }

                    });
                } else if ( ((b.op() == IMPL) || (b.op() == EQUI)) && ((b.dt() == DTERNAL) || (b.dt() == 0))) {
                    compress(b, (features, pattern) -> {

                        for (Term f : features) {
                            //after variable introduction, such implication is self-referential and probably this conjunction captures the semantics:
                            n.inputLater(
                                    print(task(b, $.conj(f, pattern)).log(getClass().getSimpleName()))
                            );
                        }
                    });
                }
            }
        }

        static Task print(Task t) {
            System.out.println(t);
            return t;
        }


        protected void compress(Termed<Compound> b, BiConsumer<List<Term>,Term> each) {
            Compound<?> bt = b.term();
            TermContainer<?> subs = bt.subterms();
            int negs = subs.count(x -> x.op() == Op.NEG);

            int subCount = bt.size();
            boolean negate = (negs == subCount);
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
            Map<ByteList,List<Term>> numbers = new HashMap();

            //first subterm: infer location of all inductables
            BiPredicate<ByteList, @Nullable Term> collect = (p, t) -> {
                if (!p.isEmpty() && t!=null) {
                    List<Term> c = numbers.computeIfAbsent(p.toImmutable(), (pp) -> Global.newArrayList(1));
                    c.add(t);
                }
                return true;
            };

            Compound<?> first = (Compound)subs.term(0);
            //first.pathsTo(ArithmeticTest::intOrNullTerm, collect);
            first.pathsTo(x -> x, collect);
            int paths = numbers.size();

            if (paths == 0)
                return;



            //analyze remaining subterms
            for (int i = 1; i < subCount; i++) {

                //if a subterm is not an integer, check for equality of atoms (structure already compared abovec)
                subs.term(i).pathsTo(
                        (e) -> e, //(e instanceof Atom && intOrNull(e)==null) ? e : null,
                        collect);
                        /*(p, x) -> {

                            if (!p.isEmpty()) { //ignore the root superterm

                                //if this is is a potential variable path, add the subterm's value at it to the list of values
                                List<Term> v = numbers.get(p);
                                if (v != null) {
                                    v.add(x);
                                } else {
                                    //else every atomic value must be consistently equal throughout
                                    if (x instanceof Atomic && !first.subterm(p).equals(x))
                                        return false;
                                }
                            }

                            return true;


                        }*/;


            }

            numbers.forEach((pp, nn) -> {

                //all subterms must contribute a value for this path
                if (nn.size()!=subCount)
                    return;

                //for each path where the other numerics are uniformly equal (only one unique value)
                if (new HashSet(nn).size()==1) {

                    numbers.forEach((ppp, nnnt) -> {

                        if (!pp.equals(ppp)) {

                            IntArrayList nnn = intsOrNull(nnnt);
                            if (nnn!=null) {

                                //System.out.println(b + " " + pp + " " + nn + " : " + "\t" + numbers + " " + pattern);

                                List<Term> features = features(nnn, vv);
                                if (!features.isEmpty()) {
                                    Term pattern = $.negIf(
                                        $.terms.transform(first, ppp, vv), negate
                                    );
                                    each.accept(features, pattern);
                                }
                            }
                        }
                    });
                }

            });


        }

        private List<Term> features(IntArrayList numbers, Term relatingVar) {
            FasterList<Term> ll = Global.newArrayList(0);
            ll.addIfNotNull(iRange(numbers, relatingVar));

            //...

            return ll;
        }


        public Term iRange(IntArrayList l, Term relatingVariable) {

            int min = l.min();
            int max = l.max();

            if (l.size() == 1) {
                throw new RuntimeException("invalid sample count");
            }

            if (min == max) {
                //return $.p($.the("intEqual"), relatingVariable, $.the(min));
                return null; //no new information needs to be provided
            }

            //return $.$("l1Dist(" + $.varDep(1) + ", " + (Math.abs(x - y)) + ")");
            return $.p($.the("intRange"), relatingVariable, $.the(min), $.the(max-min));
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
