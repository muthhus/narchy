package nars.op.software.prolog.terms;

import nars.op.software.prolog.fluents.HashDict;
import nars.op.software.prolog.io.IO;

import java.util.ArrayList;
import java.util.Iterator;


//!depends

/**
 * Term Copier agent. Has its own Variable dictionnary.
 * Uses a generic action propagator which recurses over Terms.
 */
public class Copier extends SystemObject {
    /**
     * Extracts the free variables of a Term, using a
     * generic action/reaction mechanism which takes
     * care of recursing over its structure.
     * It can be speeded up through specialization.
     */
    final static Const anAnswer = new Const("answer");
    private final HashDict dict;

    /**
     * creates a new Copier together with
     * its related HashDict for variables
     */
    Copier() {
        dict = new HashDict();
    }

    // Term copyMe(Term that) {
    // return that.reaction(this);
    // }

    /**
     * Reifies an Iterator as a ArrayList.
     * ArrayList.iterator() can give back the iterator if needed.
     *
     * @see Copier
     */
    static ArrayList EnumerationToVector(Iterator e) {
        ArrayList V = new ArrayList();
        while (e.hasNext()) {
            V.add(e.next());
        }
        return V;
    }

    public static ArrayList ConsToVector(Const Xs) {
        ArrayList V = new ArrayList();
        Term t = Xs;
        for (; ; ) {
            if (t instanceof Nil) {
                break;
            } else if (t instanceof Cons) {
                Cons c = (Cons) t;
                V.add(c.arg(0));
                t = c.arg(1);
            } else if (t instanceof Const) {
                V.add(t);
                break;
            } else {
                V = null;
                IO.error("bad Cons in ConsToVector: " + t);
                break;
            }
        }
        // IO.mes("V="+V);
        return V;
    }

    /**
     * Converts a reified Iterator to functor
     * based on name of Const c and args being the elements of
     * the Iterator.
     */

    static Term toFun(Const c, Iterator e) {
        ArrayList V = EnumerationToVector(e);
        int arity = V.size();
        if (arity == 0)
            return c;
        Fun f = new Fun(c.name(), arity);
        for (int i = 0; i < arity; i++) {
            f.args[i] = (Term) V.get(i);
        }
        return f;
    }

    /**
     * Represents a list [f,a1...,an] as f(a1,...,an)
     */

    static Fun VectorToFun(ArrayList V) {
        Const f = (Const) V.get(0);
        int arity = V.size() - 1;
        Fun T = new Fun(f.name(), arity);
        for (int i = 0; i < arity; i++) {
            T.args[i] = (Term) V.get(i + 1);
        }
        return T;
    }

    /**
     * This action only defines what happens here (at this
     * <b> place </b>).  Ageneric mechanism will be used to recurse
     * over Terms in a (truly:-)) OO style (well, looks more
     * like some Haskell stuff, but who cares).
     */
    Term action(Term place) {

        if (place instanceof Var) {
            place = (Var) dict.computeIfAbsent(place, p -> {
                return new Var();
            });
//      Var root=(Var)dict.get(place);
//      if(null==root) {
//        root=new Var();
//        dict.put(place,root);
//      }
//      place=root;
        }

        return place;
    }

    Term getMyVars(Term that) {
    /*Term*/
        that.reaction(this);
        return toFun(anAnswer, dict.keySet().iterator());
    }
}
