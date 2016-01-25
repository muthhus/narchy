package nars.nal.nal7;

import nars.Op;
import nars.term.compound.GenericCompound;
import nars.term.container.TermVector;

/**
 * "then" (adv) after that; next; afterward. in that case; therefore. a
 * relationship between 2 temporal events across time
 */
public class Then extends GenericCompound {

	public Then(Op op, TermVector subterms, int cyclesDelay) {
		super(op, subterms);
	}
}
