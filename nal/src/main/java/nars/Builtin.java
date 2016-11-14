package nars;

import nars.bag.Bag;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.SensorConcept;
import nars.nar.Default;
import nars.op.data.differ;
import nars.op.data.intersect;
import nars.op.data.reflect;
import nars.op.data.union;
import nars.term.atom.Atom;
import nars.util.Texts;
import nars.util.list.FasterList;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Date;

import static java.nio.file.Files.createTempFile;
import static nars.$.quote;
import static nars.concept.Functor.*;

/**
 * Built-in functors, ie. the standard core function set
 */
public class Builtin extends FasterList<Concept> {

    static Concept[] statik = {
            new intersect(),
            new differ(),
            new union(),
            f0("date", () -> quote(new Date().toString())),
            f1("reflect", reflect::reflect),

    };

    /**
     * generate all NAR-contextualized functors
     */
    public Builtin(NAR nar) {
        addAll(
                f0("help", () -> {
                    //TODO generalize with a predicate to filter the concepts, and a lambda for appending each one to an Appendable
                    StringBuilder sb = new StringBuilder(4096);

                    sb.append("Functions:");

                    nar.forEachConcept(x -> {
                        if (x instanceof PermanentConcept && !(x instanceof SensorConcept)) {
                            sb.append(x.toString()).append('\n');
                        }
                    });
                    return $.quote(sb);
                }),
                f0("clear", nar::clear),
                f0("reset", nar::reset),
                f0("whoami", () -> nar.self),
                f0("memstat", () -> quote(nar.concepts.summary())),
                //TODO concept statistics
                //TODO task statistics
                //TODO emotion summary
                f1("print", x -> quote(nar.concept(x).print(new StringBuilder(1024)))),
                f("save", urlOrPath -> {
                    try {
                        File tmp;
                        if (urlOrPath.length == 0) {
                            tmp = createTempFile("nar_save_", ".nal").toFile();
                        } else {
                            tmp = new File($.unquote(urlOrPath[0]));
                        }
                        PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(tmp), 64 * 1024));
                        nar.outputTasks((x) -> true, ps);
                        return quote("Saved: " + tmp.getAbsolutePath()); //TODO include # tasks, and total byte size
                    } catch (IOException e) {
                        return quote(e);//e.printStackTrace();
                    }
                }),
                f("top", (arguments) -> {
                    int MAX_RESULT_LENGTH = 800;


                    StringBuilder b = new StringBuilder();
                    @NotNull Bag<Concept> cbag = ((Default) nar).core.active;

                    String query;
                    if (arguments.length > 0 && arguments[0] instanceof Atom) {
                        query = arguments[0].toString().toLowerCase();
                    } else {
                        query = null;
                    }

                    cbag.topWhile(c -> {
                        String bs = c.get().toString();
                        if (query == null || bs.toLowerCase().contains(query)) {
                            b.append(c.get()).append('=').append(Texts.n2(c.pri())).append("  ");
                        }
                        return b.length() <= MAX_RESULT_LENGTH;
                    });

                    if (b.length() == 0)
                        return quote("(empty)");

                    return quote(b.toString());

                })
        );

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

}
