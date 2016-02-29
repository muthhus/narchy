package nars.op.sys.prolog.terms;

/**
 * Always fails
 */
class fail_ extends ConstBuiltin {
	fail_() {
		super("fail");
	}

	@Override
	public int exec(Prog p) {
		return 0;
	}
}