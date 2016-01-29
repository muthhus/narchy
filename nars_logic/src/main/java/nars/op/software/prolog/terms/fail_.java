package nars.op.software.prolog.terms;


/**
  Always fails
*/
class fail_ extends ConstBuiltin {
  fail_() {super("fail");}

  public int exec(Prog p) {
    return 0;
  }
}