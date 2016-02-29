package nars.op.sys.prolog.terms;

/**
 * Always succeeds
 */
class true_ extends ConstBuiltin {
	true_() {
		super("true");
	}

	@Override
	public int exec(Prog p) {
		return 1;
	}
}
