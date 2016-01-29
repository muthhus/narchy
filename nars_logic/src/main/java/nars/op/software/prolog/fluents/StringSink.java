package nars.op.software.prolog.fluents;

import prolog.terms.Const;
import prolog.terms.Prog;
import prolog.terms.Sink;
import prolog.terms.Term;

/**
  Builds  Fluents from Java
  Streams
*/
public class StringSink extends Sink {
  protected StringBuilder buffer;
  
  public StringSink(Prog p){
    super(p);
    this.buffer=new StringBuilder();
  }
  
  public int putElement(Term t) {
    buffer.append(t.toUnquoted());
    return 1;
  }
  
  public void stop() {
    buffer=null;
  }
  
  public Term collect() {
    return new Const(buffer.toString());
  }
}
