package nars.nal.nal7;

import nars.Op;
import nars.term.TermVector;
import nars.term.compound.GenericCompound;

/**
 * "then" (adv) after that; next; afterward. in that case; therefore. a
 * relationship between 2 temporal events across time
 */
public class Then extends GenericCompound {

	public Then(Op op, TermVector subterms, int cyclesDelay) {
		super(op, subterms);
	}
}
