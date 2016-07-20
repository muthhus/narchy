package nars.op;

import com.gs.collections.api.list.primitive.ByteList;
import com.gs.collections.impl.list.mutable.primitive.IntArrayList;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.task.GeneratedTask;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.var.GenericVariable;
import nars.util.Texts;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static nars.Op.CONJ;
import static nars.Op.EQUI;
import static nars.Op.IMPL;
import static nars.nal.Tense.DTERNAL;

/** arithmetic rule mining & variable introduction */
public class ArithmeticInduction implements Consumer<Task> {
    private final NAR n;


    public ArithmeticInduction(NAR n) {
        this.n = n;
        n.onTask(this);
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

    @Override
    public void accept(Task b) {

        if (!b.isBeliefOrGoal()) {

        } else {
            if ( (b.op() == CONJ) && ((b.dt() == DTERNAL) || (b.dt() == 0))) {

                compress(b, (features, pattern) -> {

                    n.inputLater(
                            task(b, $.conj(features, pattern)).log(getClass().getSimpleName())
                    );
                        /*n.inputLater(
                                (new MutableTask(
                                    $.sim(
                                    //$.equi(
                                        b.term(), $.conj(f, b.dt(), pattern)
                                    ),
                                '.', 1f, n).log(getClass().getSimpleName()))
                        );*/


                });
            } else if ( ((b.op() == IMPL) || (b.op() == EQUI)) && ((b.dt() == DTERNAL) || (b.dt() == 0))) {
                compress(b, (features, pattern) -> {

                    //after variable introduction, such implication is self-referential and probably this conjunction captures the semantics:
                    n.inputLater(
                            print(task(b, $.conj(features, pattern)).log(getClass().getSimpleName()))
                    );

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
                List<Term> c = numbers.computeIfAbsent(p.toImmutable(), (pp) -> $.newArrayList(1));
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

                final int[] var = {0};

                List<Term> features = $.newArrayList();
                final Compound[] pattern = {first};
                final Term[] vv = { var(0) };

                numbers.forEach((ppp, nnnt) -> {

                    if (!pp.equals(ppp)) {

                        IntArrayList nnn = intsOrNull(nnnt);
                        if (nnn!=null) {


                            //System.out.println(b + " " + pp + " " + nn + " : " + "\t" + numbers + " " + pattern);

                            List<Term> ff = features(nnn, vv[0]);
                            if (!ff.isEmpty()) {
                                features.addAll(ff);

                                //introduce variable
                                pattern[0] = (Compound) $.terms.transform(pattern[0], ppp, vv[0]);

                                //increment variable
                                vv[0] = var(++var[0]);
                            }
                        }
                    }
                });

                if (!features.isEmpty()) {
                    each.accept(features,
                            $.negIf(pattern[0], negate));
                }

            }

        });


    }

    protected final GenericVariable var(int i) {
        return new GenericVariable(Op.VAR_INDEP, Integer.toString(i));
    }

    private List<Term> features(IntArrayList numbers, Term relatingVar) {
        FasterList<Term> ll = $.newArrayList(0);
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
        return new GeneratedTask(
                tt,
                b.punc(), b.truth())
                .time(n.time(), b.occurrence())
                .budget(b)
                .evidence(b.evidence());
    }
}
