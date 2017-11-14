package nars.op.prolog;

import alice.tuprolog.*;
import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.Util;
import nars.$;
import org.eclipse.collections.impl.block.factory.PrimitiveFunctions;

public class PrologToNAL {

    public static Iterable<nars.term.Term> N(Theory t) {
        return N((Iterable)t);
    }

    public static Iterable<nars.term.Term> N(Iterable<alice.tuprolog.Term> t) {
        //System.out.println(t);

        return Iterables.transform(t, PrologToNAL::N);

//        Iterator<? extends Term> xx = t.iterator();
//        while (xx.hasNext()) {
//            Term z = xx.next();
//            System.out.println(z);
//        }
//
//        return y;
    }

    private static nars.term.Term N(alice.tuprolog.Term t) {
        if (t instanceof Struct) {
            Struct s = (Struct) t;
            String name = s.name();
            switch (name) {
                case ":-":
                    assert(s.subs()==2);
                    nars.term.Term pre = N(s.sub(1));
                    nars.term.Term post = N(s.sub(0));
                    return $.impl(pre,post);
                case ",":
                    return $.conj(N(s.sub(0)), N(s.sub(1)));
                default:
                    nars.term.Term atom = $.the(name);
                    int arity = s.subs();
                    if (arity == 0) {
                        return atom;
                    } else {
                        return $.inh(
                                $.p((nars.term.Term[])Util.map(0, arity, i -> N(s.sub(i)), nars.term.Term[]::new)),
                                atom);
                    }
            }
        } else if (t instanceof Var) {
            return $.varQuery(((Var) t).name());
            //throw new RuntimeException(t + " untranslated");
        } else if (t instanceof Int) {
            return $.the(((Int)t).intValue());
        } else {
            throw new TODO(t + " (" + t.getClass() + ") untranslatable");
        }
    }

}
