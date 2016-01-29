package nars.op.software.prolog.terms;

/**
* Template for builtins of arity >0
*/

abstract public class FunBuiltin extends Fun {
  public FunBuiltin(String f,int i){
    super(f,i);
  }
  
  abstract public int exec(Prog p);
  
  public final boolean isBuiltin() {
    return true;
  }
}
