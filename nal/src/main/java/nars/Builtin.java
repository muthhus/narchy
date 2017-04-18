package nars;

import jcog.Texts;
import jcog.pri.PLink;
import nars.concept.Concept;
import nars.op.Command;
import nars.op.DepIndepVarIntroduction;
import nars.op.data.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.obj.IntTerm;
import nars.term.transform.Functor;
import nars.term.var.Variable;
import org.junit.Assert;

import java.util.Arrays;

import static nars.$.quote;
import static nars.Op.INT;
import static nars.Op.PROD;
import static nars.Op.False;
import static nars.Op.True;
import static nars.term.Terms.compoundOrNull;

/**
 * Built-in functors, ie. the standard core function set
 */
public class Builtin {

    //TODO: http://software-lab.de/doc/ref.html#fun
    //TODO: https://openreview.net/pdf?id=ByldLrqlx (section F)

    static final Concept[] statik = {

            new intersect(),
            new differ(),
            new union(),
            new DepIndepVarIntroduction.VarIntro(),

            //Functor.f0("date", () -> quote(new Date().toString())),

            Functor.f1Const("reflect", reflect::reflect),

//            Functor.f1Const("fromJSON", (jsonString)-> IO.fromJSON($.unquote(jsonString))),
//            Functor.f1Const("toJSON", IO::toJSON),

            Functor.f1Const("toString", x -> $.quote(x.toString())),
            Functor.f1Const("toChars", x -> $.p(x.toString().toCharArray(), $::the)),
            Functor.f1Const("complexity", x -> $.the(x.complexity())),

            new flat.flatProduct(),
            new similaritree(),

            Functor.f2("equal", (x, y) ->
                    x.equals(y) ? True : ((x instanceof Variable) || (y instanceof Variable) ? null : False)),

            Functor.f2Int("add", (x, y) -> x + y),
            Functor.f2Int("sub", (x, y) -> x - y),

            Functor.f1("quote", x -> x), //TODO does this work    //throw new RuntimeException("quote should never actually be invoked by the system");

            /** slice(<compound>,<selector>)
                  selector :-
                      a specific integer value index, from 0 to compound size
                      (a,b) pair of integers, a range of indices */
            Functor.f("slice", (args) -> {
                if (args.length == 2) {
                    Compound x = compoundOrNull(args[0]);
                    if (x != null) {
                        int len = x.size();

                        Term index = args[1];
                        Op o = index.op();
                        if (o == INT) {
                            //specific index
                            int i = ((IntTerm) index).val;
                            if (i >= 0 && i < len)
                                return x.term(i);
                            else
                                return False;

                        } else if (o == PROD && index.size() == 2) {
                            Term start = ((Compound) index).term(0);
                            if (start.op() == INT) {
                                Term end = ((Compound) index).term(1);
                                if (end.op() == INT) {
                                    int si = ((IntTerm) start).val;
                                    if (si >= 0 && si < len) {
                                        int ei = ((IntTerm) end).val;
                                        if (ei >= 0 && ei <= len) {
                                            if (si == ei)
                                                return Terms.ZeroProduct;
                                            if (si < ei) {
                                                return $.p(Arrays.copyOfRange(x.terms(), si, ei));
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
            })

    };

    /**
     * generate all NAR-contextualized functors
     */
    public static void load(NAR nar) {
        //TODO these should be command-only operators, not functors



        nar.on("assertEquals", (Command) (op, args, nn) -> {
            //String msg = op + "(" + Joiner.on(',').join(args) + ')';
            Assert.assertEquals(/*msg,*/ 2, args.length);
            Assert.assertEquals(/*msg,*/ args[0], args[1]);
        });

        nar.on(Functor.f0("self", nar::self));

        nar.on(Functor.f1Concept("belief", nar, (c, n) -> $.quote(c.belief(n.time.time(), n.dur()))));
        nar.on(Functor.f1Concept("goal", nar, (c, n) -> $.quote(c.goal(n.time.time(), n.dur()))));

        nar.on("concept", (Command) (op, a, nn) -> {
            Concept c = nn.concept(a[0]);
            Command.log(nn,
                    (c != null) ?
                            quote(c.print(new StringBuilder(1024))) : $.func("unknown", a[0])
            );
        });

        Command log = (a, t, n) -> NAR.logger.info("{}", t);
        nar.on("log", log);
        nar.on(Command.LOG_FUNCTOR, log);

        nar.on("error", (Command) (a, t, n) -> NAR.logger.error("{}", t));


        nar.on("memstat", (Command) (op, a, nn) ->
            Command.log(nn, quote(nn.concepts.summary()))
        );

        nar.on("emotion", (Command) (op, a, nn) ->
            Command.log(nn, quote(nn.emotion.summary()))
        );

        nar.on("reset", (Command) (op, args1, nn) ->
            nn.runLater(NAR::reset)
        );

        nar.on("clear", (Command) (op, args, n) -> {
            n.clear();
            n.runLater(() -> {
                Command.log(n, "Ready. (" + n.concepts.size() + " subconcepts)");
            });
        });


        nar.on("top", (Command) (op, args, n) -> {
            Iterable<PLink<Concept>> ii = n.concepts();

            int MAX_RESULT_LENGTH = 250;
            StringBuilder b = new StringBuilder(MAX_RESULT_LENGTH + 8);

            if (args.length > 0 && args[0] instanceof Atom) {
                String query = $.unquote(args[0]).toLowerCase();
                for (PLink<Concept> bc : ii) {
                    String bs = bc.get().toString();
                    String cs = bs.toLowerCase();
                    if (cs.contains(query)) {
                        b.append(bs).append("  ");
                        if (b.length() > MAX_RESULT_LENGTH)
                            break;
                    }

                }
            } else {
                for (PLink<Concept> bc : ii) {
                    b.append(bc.get()).append('=').append(Texts.n2(bc.pri())).append("  ");
                    if (b.length() > MAX_RESULT_LENGTH)
                        break;
                }
            }

            Command.log(n, b.toString());
            //"core pri: " + cbag.active.priMin() + "<" + Texts.n4(cbag.active.priHistogram(new double[5])) + ">" + cbag.active.priMax());

        });


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
