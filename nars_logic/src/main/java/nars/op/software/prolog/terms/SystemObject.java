package nars.op.software.prolog.terms;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A SystemObject is a  Nonvar with system assigned name
 * 
 */
public class SystemObject extends Nonvar {
  
  static final AtomicLong ctr= new AtomicLong(0);
  
  private final long ordinal;
  
  public SystemObject(){
    ordinal= ctr.incrementAndGet();
  }
  
  public String name() {
    return '{' +getClass().getName()+ '.' +ordinal+ '}';
  }
  
  boolean bind_to(Term that,Trail trail) {
    return super.bind_to(that,trail)&&ordinal==((SystemObject)that).ordinal;
  }
  
  public String toString() {
    return name();
  }
  
  public final int getArity() {
    return Term.JAVA;
  }
}
