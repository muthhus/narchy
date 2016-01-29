package nars.op.software.prolog.terms;

import java.util.ArrayList;

abstract public class Source extends Fluent {
  
  public Source(Prog p){
    super(p);
  }
  
  abstract public Term getElement();
  
  public Const toList() {
    Term head=getElement();
    if(null==head)
      return Const.aNil;
    Cons l=new Cons(head,Const.aNil);
    Cons curr=l;
    for(;;) {
      head=getElement();
      if(null==head)
        break;
      Cons tail=new Cons(head,Const.aNil);
      curr.args[1]=tail;
      curr=tail;
    }
    return l;
  }
  
  public Term toFun() {
    ArrayList V=new ArrayList();
    Term X;
    while(null!=(X=getElement())) {
      V.add(X);
    }
    return Copier.VectorToFun(V);
  }
}
