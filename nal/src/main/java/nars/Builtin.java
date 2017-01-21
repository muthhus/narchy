package nars;

import jcog.Texts;
import nars.concept.Concept;
import nars.control.ConceptBagControl;
import nars.link.BLink;
import nars.nar.Default;
import nars.op.Command;
import nars.op.data.*;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.obj.IntTerm;
import nars.term.transform.Functor;
import nars.term.var.Variable;

import java.util.Date;

import static nars.$.quote;
import static nars.term.Term.False;
import static nars.term.Term.True;

/**
 * Built-in functors, ie. the standard core function set
 */
public class Builtin  {

    static Concept[] statik = {

            new intersect(),
            new differ(),
            new union(),

            Functor.f0("date", () -> quote(new Date().toString())),
            Functor.f1("reflect", reflect::reflect),
            Functor.f1("fromJSON", (jsonString)-> IO.fromJSON($.unquote(jsonString))),
            Functor.f1("toJSON", IO::toJSON),
            Functor.f1("toString", t -> $.quote(t.toString())),

            new reflect(),
            new flat.flatProduct(),
            new similaritree(),
            new complexity(),

            Functor.f2("equal", (x,y) ->
                x.equals(y) ? True : (!((x instanceof Variable) || (y instanceof Variable)) ? False : null)),
            Functor.f2("add", (x,y) ->
                ((x instanceof IntTerm) && (y instanceof IntTerm)) ?
                    $.the(((IntTerm)x).val + ((IntTerm)y).val) : null),
            Functor.f2("sub", (x,y) ->
                ((x instanceof IntTerm) && (y instanceof IntTerm)) ?
                    $.the(((IntTerm)x).val - ((IntTerm)y).val) : null),
    };

    /**
     * generate all NAR-contextualized functors
     */
    public static void load(NAR nar) {
        //TODO these should be command-only operators, not functors

        nar.on(Functor.f0("self", nar::self));

        nar.on(Functor.f1c("belief", nar, (c,n) -> $.quote(c.belief(n.time()))));
        nar.on(Functor.f1c("goal", nar, (c,n) -> $.quote(c.goal(n.time()))));

        nar.on("concept", (Command) (op, a, nar1) ->
                Command.log(nar,
                        quote(nar.concept(a[0]).print(new StringBuilder(1024))))
        );

        Command log = (a, t, n) -> n.logger.info("{}", t);
        nar.on("log", log);
        nar.on(Command.LOG_FUNCTOR, log);

        nar.on("error", (Command) (a, t, n) -> n.logger.error("{}", t) );

        nar.on("memstat", (Command) (op, a, nn) ->
            Command.log(nar, quote(nar.concepts.summary()))
        );

        nar.on("reset", (Command) (op, args1, nar1) ->
            nar1.runLater(NAR::reset)
        );

        nar.on("clear", (Command) (op, args, n) -> {
            n.clear();
            n.runLater(()->{
                Command.log(n, "Ready. (" + n.concepts.size() + " subconcepts)");
            });
        });



        nar.on("top", (Command) (op, args, n) -> {
            Iterable<BLink<Concept>> ii = n.conceptsActive();

            int MAX_RESULT_LENGTH = 200;
            StringBuilder b = new StringBuilder();

            if (args.length > 0 && args[0] instanceof Atom) {
                String query = args[0].toString().toLowerCase();
                for (BLink<Concept> bc : ii) {
                    String bs = bc.get().toString();
                    String cs = bs.toLowerCase();
                    if (cs.contains(query)) {
                        b.append(bs).append("  ");
                        if (b.length() > MAX_RESULT_LENGTH)
                            break;
                    }

                }
            } else {
                for (BLink<Concept> bc : ii) {
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
