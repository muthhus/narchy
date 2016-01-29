package nars.op.software.prolog.terms;


/**
  List Constructor. Cooperates with terminator Nil.

  @see Nil
*/
public class Cons extends Fun {
  public Cons(String cons,Term x0,Term x1){
    super(cons,x0,x1);
  }
  
  public Cons(Term x0,Term x1){
    this(".",x0,x1);
  }
  
  public Term getHead() {
    return getArg(0);
  }
  
  public Term getTail() {
    return getArg(1);
  }
  
  /**
    List printer.
  */
  public String toString() {
    Term h=getArg(0);
    Term t=getArg(1);
    StringBuilder s=new StringBuilder('[' +watchNull(h));
    for(;;) {
      if(t instanceof Nil) {
        s.append(']');
        break;
      } else if(t instanceof Cons) {
        h=((Cons)t).getArg(0);
        t=((Cons)t).getArg(1);
        s.append(',').append(watchNull(h));
      } else {
        s.append('|').append(watchNull(t)).append(']');
        break;
      }
    }
    return s.toString();
  }
}
