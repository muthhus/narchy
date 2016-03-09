package nars.op;

import clojure.lang.*;
import com.google.common.collect.Lists;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;

import java.util.Arrays;

/**
 * Created by me on 3/9/16.
 */
public class Narjure extends Dynajure {


//    public Narjure() {
//        Symbol.intern("\"-->\"");
//        eval("(defn \"-->\" [subterms] nil)");
//    }

    /**
     * temporary translation method
     */
    @Deprecated
    public Term clojureToNars(Object o) {
        //if (o instanceof Object[])
            //System.out.println(Arrays.toString((Object[]) o));
        //System.out.println(o + " " + o.getClass());
        return o == null ? null : Atom.the(o.toString());
    }

    static final IFn quote = (IFn) RT.readString("quote");

    /**
     * temporary translation method
     */
    @Deprecated
    public Object narsToClojure(Object o) {
        if (o instanceof String) {
            return RT.readString(o.toString());
        } else if (o instanceof Atomic) {
            Atomic a = (Atomic) o;

            //String as = a.toStringUnquoted();

            return narsToClojure(a.toString());
            //return Symbol.intern(as);
        } else if ((o instanceof Compound) && (((Compound) o).op() != Op.PRODUCT)) {
            //Non-Product compounds

            return
                //RT.list(
                    //quote,
                    //RT.list(
                    Tuple.create(
                        narsToClojure("\"" + (((Compound) o)).op().str + "\""),     //TODO cache these in array for fast lookup
                        narsToClojure(((Compound) o).subterms())
                    )
                //)
            ;

        } else if (o instanceof TermContainer) {
            //generic TermContainers and Product compounds
            return narsToClojure(((TermContainer) o).terms());
        } else if (o instanceof Object[]) {
            Object[] a = (Object[]) o;
            int alen = a.length;
            Object[] cc = new Object[alen];
            for (int i = 0; i < alen; i++) {
                cc[i] = narsToClojure(a[i]);
            }
            return PersistentList.create(Lists.newArrayList(cc));
            //return cc;
        }

        throw new RuntimeException("Untranslated: " + o);
    }

    public Term eval(Termed x) {
        return clojureToNars(eval(narsToClojure(x.term())));
    }
}
