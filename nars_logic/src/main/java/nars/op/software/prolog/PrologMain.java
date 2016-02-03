package nars.op.software.prolog;

import nars.op.software.prolog.builtins.Builtins;

/**
 * Minimal command line only Prolog main entry point
 */
public class PrologMain extends Prolog {

	public PrologMain() {

		dict = new Builtins(this);
		ask("reconsult('"
				+ PrologMain.class.getResource(Prolog.default_lib)
						.toExternalForm() + "')");
	}

	public static void main(String args[]) {
		new PrologMain().run(args).standardTop();
		// if(0==init())
		// return;
		// if(!Init.run(args))
		// return;
		// Init.standardTop(); // interactive
	}
}
