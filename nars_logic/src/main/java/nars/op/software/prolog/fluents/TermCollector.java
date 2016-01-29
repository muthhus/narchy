package nars.op.software.prolog.fluents;

import prolog.terms.Prog;
import prolog.terms.Sink;
import prolog.terms.Term;

import java.util.ArrayList;

/**
  Builds  Fluents from Java
  Streams
*/
public class TermCollector extends Sink {
  protected ArrayList buffer;
  
  private final Prog p;
  
  public TermCollector(Prog p){
    super(p);
    this.p=p;
    this.buffer=new ArrayList();
  }
  
  public int putElement(Term T) {
    buffer.add(T);
    return 1;
  }
  
  public void stop() {
    buffer=null;
  }
  
  public Term collect() {
    return new JavaSource(buffer,p);
  }
}
