package nars.op.software.prolog.terms;

/**
  Special constant terminating a list
*/
public final class Nil extends Const {
  Nil(String s){
    super(s);
  }
  
  public Nil(){
    this("[]");
  }
}
