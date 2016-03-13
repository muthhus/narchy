package nars.op;

import clojure.lang.Dynajure;
import clojure.lang.PersistentList;
import clojure.lang.RT;
import clojure.lang.Tuple;
import com.google.common.collect.Lists;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;

/**
 * Created by me on 3/9/16.
 */
public class Narjure extends Dynajure {


    public Narjure() {
        //Symbol.intern("\"-->\"");

        //eval("(defn INHERITANCE [a b] nil)");

    }

    /**
     * temporary translation method
     */
    @Deprecated
    public Term clojureToNars(Object o) {
        //if (o instanceof Object[])
            //System.out.println(Arrays.toString((Object[]) o));
        //System.out.println(o + " " + o.getClass());
        return Atom.the(o);
    }

    //static final IFn quote = (IFn) RT.readString("quote");

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
                        narsToClojure("\"" + (((Compound) o)).op().toString() + "\""),     //TODO cache these in array for fast lookup
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
        return x == null ? null : eval((Object) x.term());
    }

    @Override
    public Term eval(Object x) {
//        if (x instanceof Term) {
//            return (Term)x;
//        }
        if ((x instanceof Number) || (x instanceof String))  {
            return Atom.the(x);
        }

        x = narsToClojure(x);
        return clojureToNars(super.eval(x));
    }
}
