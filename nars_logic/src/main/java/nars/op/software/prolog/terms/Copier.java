package nars.op.software.prolog.terms;

import nars.Global;
import nars.op.software.prolog.io.IO;

import java.util.ArrayList;
import java.util.Map;

//!depends

/**
 * Term Copier agent. Has its own Variable dictionnary. Uses a generic action
 * propagator which recurses over Terms.
 */
public class Copier extends SystemObject {
	/**
	 * Extracts the free variables of a Term, using a generic action/reaction
	 * mechanism which takes care of recursing over its structure. It can be
	 * speeded up through specialization.
	 */
	final static Const anAnswer = new Const("answer");
	private final Map<PTerm,Var> dict;

	final static String COPIER_PREFIX = "c";
	/**
	 * creates a new Copier together with its related HashDict for variables
	 */
	Copier() {
		super(COPIER_PREFIX);
		dict = Global.newHashMap();
	}

	// Term copyMe(Term that) {
	// return that.reaction(this);
	// }

	public static ArrayList ConsToVector(Nonvar Xs) {
		ArrayList V = new ArrayList();
		PTerm t = Xs;
		for (;;) {
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
	 * Represents a list [f,a1...,an] as f(a1,...,an)
	 */

	static Fun VectorToFun(ArrayList V) {
		Const f = (Const) V.get(0);
		int arity = V.size() - 1;
		Fun T = new Fun(f.name, arity);
		for (int i = 0; i < arity; i++) {
			T.args[i] = (PTerm) V.get(i + 1);
		}
		return T;
	}

	/**
	 * This action only defines what happens here (at this <b> place </b>).
	 * Ageneric mechanism will be used to recurse over Terms in a (truly:-)) OO
	 * style (well, looks more like some Haskell stuff, but who cares).
	 */
	PTerm action(PTerm place) {

        if (place instanceof Var) {
            place = dict.computeIfAbsent(place, p -> new Var());

//      Var root=(Var)dict.get(place);
//      if(null==root) {
//        root=new Var();
//        dict.put(place,root);
//      }
//      place=root;
        }

        return place;
    }
	PTerm getMyVars(PTerm that) {
		/* Term */
		that.reaction(this);

		if (dict.isEmpty())
			return anAnswer;

		int arity = dict.size();
		PTerm[] t = new PTerm[arity];
		final int[] i = {0};
		dict.forEach( (k,v) -> {
			t[i[0]++] = v;
		});

		return new Fun(anAnswer.name, t);
	}
}
