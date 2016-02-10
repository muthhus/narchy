package nars.op.software.prolog;

import nars.op.software.prolog.builtins.Builtins;
import nars.op.software.prolog.io.Parser;
import nars.op.software.prolog.terms.PTerm;

/**
 * AXR Axiomatic Reasoner - deterministic, programmable, mostly reliable
 * counterpart to NAR
 */
public class AXR extends Prolog {

	public AXR() {

		dict = new Builtins(this);
		ask("reconsult('"
				+ AXR.class.getClassLoader().getResource(Prolog.default_lib)
						.toExternalForm() + "')");
	}

	public static void main(String args[]) {
		new AXR().run(args).standardTop();
		// if(0==init())
		// return;
		// if(!Init.run(args))
		// return;
		// Init.standardTop(); // interactive
	}

	/** introduce a fact */
	public PTerm add(String factString) {
		return db.add( Parser.stringToClause(this, factString) );
	}
}
