package nars.op.software.prolog.terms;


/**
  Special constants, used to Name variables
  @see Term
  @see Var
*/
class PseudoVar extends Const {
  PseudoVar(int i){
    super("V_"+i);
  }
  
  PseudoVar(String s){
    super(s);
  }
  
  public String toString() {
    return name();
  }
}
