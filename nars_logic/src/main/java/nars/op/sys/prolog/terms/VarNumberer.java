package nars.op.sys.prolog.terms;

import nars.Global;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Used in implementing uniform replacement of variables with new constants.
 * useful for printing out with nicer variable names.
 * 
 * @see Var
 * @see Clause
 */
class VarNumberer extends SystemObject {

	@NotNull
	final Map<PTerm,PseudoVar> dict;

	int ctr;

	VarNumberer() {
		super("VarNumberer");
		dict = Global.newHashMap();
		ctr = 0;
	}

	final PTerm action(PTerm place) {
		place = place.ref();
		// IO.trace(">>action: "+place);
		if (place instanceof Var) {
			place = dict.computeIfAbsent(place, p-> new PseudoVar(ctr++));
		}
		// IO.trace("<<action: "+place);
		return place;
	}
}
