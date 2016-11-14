package nars;

import nars.concept.Concept;
import nars.concept.Functor;
import nars.op.data.differ;
import nars.op.data.intersect;
import nars.op.data.reflect;
import nars.op.data.union;
import nars.util.data.list.FasterList;

import java.util.ArrayList;
import java.util.Date;

import static nars.concept.Functor.f;
import static nars.concept.Functor.f0;
import static nars.concept.Functor.f1;

/**
 * Built-in functors, ie. the standard core function set
 */
public class Builtin extends FasterList<Concept> {

    static Concept[] statik = {
        new intersect(),
        new differ(),
        new union(),
        f0("date", () -> $.quote(new Date().toString())),
        f1("reflect", reflect::reflect),

    };

    /** generate all NAR-contextualized functors */
    public Builtin(NAR nar) {
        addAll(
            f0("memstat", () -> $.quote(nar.concepts.summary())),
            f1("print", x -> $.quote(nar.concept(x).print(new StringBuilder(1024 ))))
        );
    }

}
