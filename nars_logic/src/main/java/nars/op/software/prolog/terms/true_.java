package nars.op.software.prolog.terms;


/**
  Always succeeds
*/
class true_ extends ConstBuiltin {
  true_(){
    super("true");
  }
  
  public int exec(Prog p) {
    return 1;
  }
}
