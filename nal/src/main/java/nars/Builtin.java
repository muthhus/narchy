package nars;

import jcog.Texts;
import nars.concept.Concept;
import nars.op.DepIndepVarIntroduction;
import nars.op.Operator;
import nars.op.Subst;
import nars.op.data.*;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Int;
import nars.term.container.TermContainer;
import nars.term.var.Variable;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

import static nars.Op.*;
import static nars.term.Functor.f0;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Built-in functors, ie. the standard core function set
 */
public class Builtin {

    //TODO: http://software-lab.de/doc/ref.html#fun
    //TODO: https://openreview.net/pdf?id=ByldLrqlx (section F)

    public static final Concept[] statik = {

            intersect.the,
            differ.the,
            union.the,
            Subst.the,

            Functor.f1("varIntro", (x) -> {
                Term y = DepIndepVarIntroduction.varIntro(x, ThreadLocalRandom.current());
                if (y == null)
                    return x; //no variables introduced, fall-through
                else
                    return y;
            }),

            /** applies the changes in structurally similar terms "from" and "to" to the target term */
            Functor.f3((Atom)$.the("substDiff"), (target, from, to) -> {
                if (from.equals(to))
                    return Null; //only interested in when there is a difference to apply

                int n;
                if (from.op() == to.op() && (n = from.subs()) == to.subs()) {
                    //likely they have the same structure
                    Map<Term,Term> m = null; //lazy alloc
                    for (int i = 0; i < n; i++) {
                        Term f = from.sub(i);
                        Term t = to.sub(i);
                        if (!f.equals(t)) {
                            if (m == null) m = new UnifiedMap(1);
                            m.put(f, t);
                        }
                    }
                    if (m!=null) { //can be empty in 'dt' cases
                        Term y = target.replace(m);
                        if (y!=null && !y.equals(target))
                            return y;
                    }
                }
                return Null;
            }),

            /** similar to C/Java "indexOf" but returns a set of all numeric indices where the 2nd argument occurrs as a subterm of the first
             *  if not present, returns Null
             * */
            //Functor.f2("numIndicesOf", (x,y) -> {
            Functor.f2("indicesOf", (x,y) -> {
                int s = x.subs();
                if (s > 0) {
                    TreeSet<Term> indices = null; //lazy alloc
                    for (int i = 0; i < s; i++) {
                        if (x.sub(i).equals(y)) {
                            if (indices == null) indices = new TreeSet();
                            indices.add(Int.the(i));
                        }
                    }
                    if (indices == null)
                        return Null;
                    else {
                        return $.sete(indices);
                        //return $.secte(indices);
                    }
                }
                return Null;
            }),
            Functor.f2("keyValues", (x,y) -> {
            //Functor.f2("indicesOf", (x,y) -> {
                int s = x.subs();
                if (s > 0) {
                    TreeSet<Term> indices = null; //lazy alloc
                    for (int i = 0; i < s; i++) {
                        if (x.sub(i).equals(y)) {
                            if (indices == null) indices = new TreeSet();
                            indices.add($.p(y, Int.the(i)));
                        }
                    }
                    if (indices == null)
                        return Null;
                    else {
                        switch (indices.size()) {
                            case 0: return Null; //shouldnt happen
                            case 1: return indices.first();
                            default:
                                //return $.secte(indices);
                                return $.sete(indices);
                        }
                    }
                }
                return Null;
            }),

            Functor.f2("varMask", (x,y) -> {
                int s = x.subs();
                if (s > 0) {
                    Term[] t = new Term[s];
                    for (int i = 0; i < s; i++) {
                        t[i] = x.sub(i).equals(y) ? y : $.varDep("z" + i); //unnormalized vars
                    }
                    return $.p(t);
                }
                return Null;
            }),

            //Functor.f0("date", () -> quote(new Date().toString())),

            Functor.f1Const("reflect", reflect::reflect),

//            Functor.f1Const("fromJSON", (jsonString)-> IO.fromJSON($.unquote(jsonString))),
//            Functor.f1Const("toJSON", IO::toJSON),

            Functor.f1Const("toString", x -> $.quote(x.toString())),
            Functor.f1Const("toChars", x -> $.p(x.toString().toCharArray(), $::the)),
            Functor.f1Const("complexity", x -> $.the(x.complexity())),

            Functor.r0("shutdown", () -> () -> System.exit(0)),


            new flat.flatProduct(),
            new similaritree(),

            Functor.f2("equal", (x, y) -> {
                if (x.equals(y))
                    return True; //unconditionally true
                if ((x.vars() > 0) || (y.vars() > 0)) {
                    return null; //unknown, fall-through
                }
                return False;
            }),

            //TODO for binding equal values
//            Functor.f2("equality", (x, y) ->
//                    x.equals(y) ? True : ((x instanceof Variable) || (y instanceof Variable) ? null :
//                    --(x diff y) && --(y diff x) )),

            Functor.f2("subterm", (Term x, Term index) -> {
                try {
                    if (index instanceof Int) {
                        return x.sub($.intValue(index));
                    }
                } catch (NumberFormatException ignored) {
                }
                return null;
            }),

            Functor.f2Int("add", (x, y) -> x + y),
            //Functor.f2Int("sub", (x, y) -> x - y),


            Functor.f1("quote", x -> x) //TODO does this work    //throw new RuntimeException("quote should never actually be invoked by the system");
    };


    /**
     * generate all NAR-contextualized functors
     */
    public static void load(NAR nar) {
        for (Concept t : Builtin.statik) {
            nar.on(t);
        }

//        nar.on(Functor.f("service", (TermContainer c) ->
//                $.sete(
//                        nar.services().map(
//                                (e) ->
//                                        $.p(e, $.the(e.getValue().state())))
//                                .toArray(Term[]::new)
//                )
//        ));

        /** subterm, but specifically inside an ellipsis. otherwise pass through */
        nar.on(Functor.f("esubterm", (TermContainer c) -> {


            Term x = c.sub(0, null);
            if (x == null)
                return Null;

            Term index = c.sub(1, Null);
            if (index == Null)
                return Null;

            int which;
            if (index != null) {
                if (index instanceof Variable)
                    return Null;

                which = $.intValue(index, -1);
                if (which < 0) {
                    return Null;
                }
            } else {
                //random
                which = nar.random().nextInt(x.subs());
            }

            return x.sub(which);


        }));

        nar.on(Functor.f2((Atom) $.the("without"), (Term container, Term content) -> {
            Term t = Op.without(container, content);
            if (t == null)
                return Null; //wasnt contained
            return t;
        }));

        /**
         * TODO rename this to 'dropAnyCommutive'
         * remove an element from a commutive conjunction (or set), at random, and try re-creating
         * the compound. wont necessarily work in all situations.
         * TODO move the type restriction to another functor to wrap this
         *
         * this also filter a single variable (depvar) from being a result
         */
        nar.on(Functor.f1((Atom) $.the("dropAnySet"), (Term t) -> {
            Op oo = t.op();

            if (oo == INT) {
                if (t instanceof Int.IntRange) {
                    //select random location in the int and split either up or down
                    Int.IntRange i = (Int.IntRange)t;
                    Random rng = nar.random();
                    if (i.min+1 == i.max) {
                        //arity=2
                        return Int.the(rng.nextBoolean() ? i.min : i.max);
                    } else if (i.min+2 == i.max) {
                        //arity=3
                        switch (rng.nextInt(4)) {
                            case 0: return Int.the(i.min);
                            case 1: return Int.range(i.min, i.min+1);
                            case 2: return Int.range(i.min+1, i.min+2);
                            case 3: return Int.the(i.max);
                            default:
                                throw new UnsupportedOperationException();
                        }
                    } else {
                        int split =
                                (i.max + i.min)/2; //midpoint, deterministic
                                //rng.nextInt(i.max-i.min-2);
                        return (rng.nextBoolean()) ?
                                Int.range(i.min, split+1) :
                                Int.range(split+1, i.max);
                    }
                }

                //cant drop int by itself
                return Null;
            }

            if (!oo.in(SETi.bit | SETe.bit))
                return Null;//returning the original value may cause feedback loop in callees expcting a change in value

            int size = t.subs();
            switch (size) {
                case 0:
                    assert (false) : "empty set impossible here";
                    return Null;
                case 1:
                    return Null; /* can't shrink below one element */
                case 2:
                    int n = nar.random().nextInt(2);
                    return oo.the(t.sub(n)) /* keep the remaining term wrapped in a set */;
                default:
                    Term[] y = dropRandom(nar.random(), t.subterms());
                    return oo.the(y);
            }
        }));

        /** depvar cleaning from commutive conj */
        nar.on(Functor.f1((Atom) $.the("ifConjCommNoDepVars"), (Term t) -> {
            if (!t.hasAny(VAR_DEP))
                return t;
            Op oo = t.op();
            if (oo != CONJ)
                return t;


            TreeSet<Term> s = t.subterms().toSortedSet();
            if (!s.removeIf(x -> x.unneg().op()==VAR_DEP))
                return t;

            return CONJ.the(t.dt(), s);
        }));

        /** drops a random contained event, whether at first layer or below */
        nar.on(Functor.f1((Atom) $.the("dropAnyEvent"), (Term t) -> {
            Op oo = t.op();
            if (oo != CONJ)
                return Null;//returning the original value may cause feedback loop in callees expcting a change in value

            int tdt = t.dt();
            Term r;
            if (tdt == DTERNAL || tdt == 0 || tdt == XTERNAL) {
                switch (t.subs()) {
                    case 0:
                    case 1:
                        throw new RuntimeException("degenerate conjunction cases");

                    case 2:
                        r = t.sub(nar.random().nextInt(2)); //one of the two
                        break;

                    default:
                        r = CONJ.the(tdt, dropRandom(nar.random(), t.subterms()));
                        break;
                }
            } else {
                //recursive event-based decomposition and recomposition

                List<LongObjectPair<Term>> ee = t.eventList();
                int toRemove = nar.random().nextInt(ee.size());
                ee.remove(toRemove);

                r = Op.conj(ee);

            }

//            if (r instanceof Variable /*&& r.op()!=VAR_DEP*/)
//                return Null; //HACK dont allow returning a variable as an event during decomposition HACK TODO make more careful and return the only result if one subterm is a non-returnable variable

            return r;
        }));
        nar.on(Functor.f2((Atom) $.the("conjEvent"), (Term c, Term when) -> {
            if (c.op() != CONJ || !(when instanceof Atom))
                return Null;
            if (c.dt() == DTERNAL || c.dt() == 0) {
                return c.sub(nar.random().nextInt(c.subs())); //choose a subterm at random
            }
            assert (c.subs() == 2);
            int target;
            switch (when.toString()) {
                case "early":
                    target = 0;
                    break;
                case "late":
                    target = 1;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            if (c.dt() < 0)
                target = 1 - target;
            return c.sub(target);
        }));

        nar.on(Functor.f2((Atom) $.the("conjDropIfEarliest"), (Term conj, Term event) -> {
            if (conj.op() != CONJ || conj.subTimeSafe(event) != 0)
                return Null;
            if (conj.dt() != DTERNAL) {
                FastList<LongObjectPair<Term>> events = conj.eventList();
                assert (events.get(0).getOne() == 0);
                LongObjectPair<Term> first = events.get(0);
                Term firstTerm = first.getTwo();
//
//                boolean neg;
                if (!firstTerm.equals(event)) {
                    return Null;
                }

                events.remove(0);
                return Op.conj(events);//.negIf(neg);
            } else {
                return Op.without(conj, event);
            }
        }));

        nar.onOp("assertEquals", (task, nn) -> {
            //String msg = op + "(" + Joiner.on(',').join(args) + ')';
            @Nullable TermContainer args = Operator.args(task);
            if (args.subs()==2) {
                //assertEquals(/*msg,*/ 2, args.subs());
                assertEquals(/*msg,*/ args.sub(0), args.sub(1));
            }
        });

        nar.on(f0("self", nar::self));


        nar.on(Functor.f1Concept("belief", nar, (c, n) -> $.quote(n.belief(c.term(), n.time()))));
        nar.on(Functor.f1Concept("goal", nar, (c, n) -> $.quote(n.goal(c.term(), n.time()))));

//        nar.on("concept", (Operator) (op, a, nn) -> {
//            Concept c = nn.concept(a[0]);
//            Command.log(nn,
//                (c != null) ?
//                    quote(c.print(new StringBuilder(1024))) : $.func("unknown", a[0])
//            );
//        });

        BiConsumer<Task, NAR> log = (t, n) -> NAR.logger.info(" {}", t);
        nar.onOp("log", log);
        nar.onOp(Operator.LOG_FUNCTOR, log);

        nar.onOpArgs("error", (t, n) -> NAR.logger.error(" {}", t));

        nar.onOp("reset", (t, nn) -> {
            nn.runLater(nn::reset);
        });

        nar.onOp("clear", (t, n) -> {
            n.runLater(() -> {
                n.clear();
                n.input(Operator.log(n.time(), "ready"));
            });
        });

        nar.onOpLogged("stat", (t, n) ->
                $.p(
                        $.quote(n.emotion.summary()),
                        $.quote(n.terms.summary()),
                        $.quote(n.emotion.summary()),
                        $.quote(n.exe.toString())
                )
        );

        nar.on(Functor.f("top", (args) -> {

            String query;
            if (args.subs() > 0 && args.sub(0) instanceof Atom) {
                query = $.unquote(args.sub(0)).toLowerCase();
            } else {
                query = null;

            }

            int MAX_RESULT_LENGTH = 10;
            List<Term> rows = $.newArrayList(MAX_RESULT_LENGTH);
            //TODO use Exe stream() methods
            nar.conceptsActive().forEach(bc -> {
                if (rows.size() < MAX_RESULT_LENGTH && (query == null || bc.toString().toLowerCase().contains(query))) {
                    rows.add($.p(
                            bc.get().term(),
                            $.the('$' + Texts.n4(bc.pri())))
                    );
                }
            });
            return $.p(rows);

//            else
//                for (PLink<Concept> bc : ii) {
//                    b.append(bc.get()).append('=').append(Texts.n2(bc.pri())).append("  ");
//                    if (b.length() > MAX_RESULT_LENGTH)
//                        break;
//                }
//            }

            //Command.log(n, b.toString());
            //"core pri: " + cbag.active.priMin() + "<" + Texts.n4(cbag.active.priHistogram(new double[5])) + ">" + cbag.active.priMax());

        }));
//

//            /** slice(<compound>,<selector>)
//            selector :-
//            a specific integer value index, from 0 to compound size
//            (a,b) pair of integers, a range of indices */
        nar.on(Functor.f("slice", (args) -> {
            if (args.subs() == 2) {
                Term x = args.sub(0);
                if (x.subs() > 0) {
                    int len = x.subs();

                    Term index = args.sub(1);
                    Op o = index.op();
                    if (o == INT) {
                        //specific index
                        int i = ((Int) index).id;
                        if (i >= 0 && i < len)
                            return x.sub(i);
                        else
                            return False;

                    } else if (o == PROD && index.subs() == 2) {
                        Term start = (index).sub(0);
                        if (start.op() == INT) {
                            Term end = (index).sub(1);
                            if (end.op() == INT) {
                                int si = ((Int) start).id;
                                if (si >= 0 && si < len) {
                                    int ei = ((Int) end).id;
                                    if (ei >= 0 && ei <= len) {
                                        if (si == ei)
                                            return Op.ZeroProduct;
                                        if (si < ei) {
                                            return $.p(Arrays.copyOfRange(x.subterms().arrayClone(), si, ei));
                                        }
                                    }
                                }
                                //TODO maybe reverse order will return reversed subproduct
                                return False;
                            }
                        }

                    }
                }
            }
            return null;
        }));


//                Functor.f0("help", () -> {
//                    //TODO generalize with a predicate to filter the concepts, and a lambda for appending each one to an Appendable
//                    StringBuilder sb = new StringBuilder(4096);
//
//                    sb.append("Functions:");
//
//                    nar.forEachConcept(x -> {
//                        if (x instanceof PermanentConcept && !(x instanceof SensorConcept)) {
//                            sb.append(x.toString()).append('\n');
//                        }
//                    });
//                    return $.quote(sb);
//                }),

//                //TODO concept statistics
//                //TODO task statistics
//                //TODO emotion summary
//                Functor.f("save", urlOrPath -> {
//                    try {
//                        File tmp;
//                        if (urlOrPath.length == 0) {
//                            tmp = createTempFile("nar_save_", ".nal").toFile();
//                        } else {
//                            tmp = new File($.unquote(urlOrPath[0]));
//                        }
//                        PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(tmp), 64 * 1024));
//                        nar.outputTasks((x) -> true, ps);
//                        return quote("Saved: " + tmp.getAbsolutePath()); //TODO include # tasks, and total byte size
//                    } catch (IOException e) {
//                        return quote(e);//e.printStackTrace();
//                    }
//                }),


//        nar.on("nar", (terms) -> {
//            //WARNING this could be dangerous to allow open access
//            Term t = terms[0];
//            if (t.op().var) {
//                Set<Term> pp = new TreeSet();
//                for (Field f : ff) {
//                    if (classWhitelist.contains(f.getType())) {
//                        try {
//                            pp.add(func("nar", the(f.getName()), the(f.get(nar))));
//                        } catch (IllegalAccessException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//                return parallel(pp);
//            } else {
//                String expr = unquote(t);
//                Object r;
//                try {
//                    r = Ognl.getValue(expr, nar);
//                } catch (OgnlException e) {
//                    r = e;
//                }
//                if (r instanceof Termed)
//                    return ((Termed) r).term();
//                else
//                    return the(r.toString());
//            }
//        });

    }

    static Term[] dropRandom(Random random, TermContainer t) {
        int size = t.subs();
        assert (size > 1);
        Term[] y = new Term[size - 1];
        int except = random.nextInt(size);
        for (int i = 0, j = 0; i < size; i++) {
            if (i != except) {
                y[j++] = t.sub(i);
            }
        }
        return y;
    }


//    public final AbstractOperator[] defaultOperators = {
//
//            //system control
//
//            //PauseInput.the,
//            new reset(),
//            //new eval(),
//            //new Wait(),
//
////            new believe(),  // accept a statement with a default truth-value
////            new want(),     // accept a statement with a default desire-value
////            new wonder(),   // find the truth-value of a statement
////            new evaluate(), // find the desire-value of a statement
//            //concept operations for internal perceptions
////            new remind(),   // create/activate a concept
////            new consider(),  // do one inference step on a concept
////            new name(),         // turn a compount term into an atomic term
//            //new Abbreviate(),
//            //new Register(),
//
//            //new echo(),
//
//
//            new doubt(),        // decrease the confidence of a belief
////            new hesitate(),      // decrease the confidence of a goal
//
//            //Meta
//            new reflect(),
//            //new jclass(),
//
//            // feeling operations
//            //new feelHappy(),
//            //new feelBusy(),
//
//
//            // math operations
//            //new length(),
//            //new add(),
//
//            new intToBitSet(),
//
//            //new MathExpression(),
//
//            new complexity(),
//
//            //Term manipulation
//            new flat.flatProduct(),
//            new similaritree(),
//
//            //new NumericCertainty(),
//
//            //io operations
//            new say(),
//
//            new schizo(),     //change Memory's SELF term (default: SELF)
//
//            //new js(), //javascript evalaution
//
//            /*new json.jsonfrom(),
//            new json.jsonto()*/
//         /*
//+         *          I/O operations under consideration
//+         * observe          // get the most active input (Channel ID: optional?)
//+         * anticipate       // get the input matching a given statement with variables (Channel ID: optional?)
//+         * tell             // output a judgment (Channel ID: optional?)
//+         * ask              // output a question/quest (Channel ID: optional?)
//+         * demand           // output a goal (Channel ID: optional?)
//+         */
//
////        new Wait()              // wait for a certain number of clock cycle
//
//
//        /*
//         * -think            // carry out a working cycle
//         * -do               // turn a statement into a goal
//         *
//         * possibility      // return the possibility of a term
//         * doubt            // decrease the confidence of a belief
//         * hesitate         // decrease the confidence of a goal
//         *
//         * feel             // the overall happyness, average solution quality, and predictions
//         * busy             // the overall business
//         *
//
//
//         * do               // to turn a judgment into a goal (production rule) ??
//
//         *
//         * count            // count the number of elements in a set
//         * arithmatic       // + - * /
//         * comparisons      // < = >
//         * logic        // binary logic
//         *
//
//
//
//         * -assume           // local assumption ???
//         *
//         * observe          // get the most active input (Channel ID: optional?)
//         * anticipate       // get input of a certain pattern (Channel ID: optional?)
//         * tell             // output a judgment (Channel ID: optional?)
//         * ask              // output a question/quest (Channel ID: optional?)
//         * demand           // output a goal (Channel ID: optional?)
//
//
//        * name             // turn a compount term into an atomic term ???
//         * -???              // rememberAction the history of the system? excutions of operatons?
//         */
//    };
//
//

}
