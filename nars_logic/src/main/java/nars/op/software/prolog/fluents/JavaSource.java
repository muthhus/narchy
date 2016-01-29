package nars.op.software.prolog.fluents;

import nars.op.software.prolog.terms.Prog;
import nars.op.software.prolog.terms.Source;
import nars.op.software.prolog.terms.Term;

import java.util.ArrayList;
import java.util.Iterator;

/**
  Builds Prolog Iterators from Java
  Sequences and Iterator type classes
*/
public class JavaSource extends Source {
  private Iterator e;
  
  public JavaSource(Prog p){
    super(p);
    e=null;
  }
  
  public JavaSource(Iterator iterator,Prog p){
    super(p);
    this.e=iterator;
  }
  
  public JavaSource(ArrayList V,Prog p){
    super(p);
    this.e=V.iterator();
  }
  
  public Term getElement() {
    if(null==e||!e.hasNext())
      return null;
    else
      return (Term)e.next();
  }
  
  public void stop() {
    e=null;
  }
}
