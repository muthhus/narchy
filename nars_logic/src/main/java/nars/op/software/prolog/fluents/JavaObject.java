package nars.op.software.prolog.fluents;

import nars.op.software.prolog.terms.SystemObject;

public class JavaObject extends SystemObject {
  public JavaObject(Object i){
    // available=true;
    val=i;
  }
  
  final Object val;
  
  public Object toObject() {
    return val;
  }
  
  /*
  private boolean available;

  synchronized public void suspend() {
    available=false;
    while(!available) {
      try {
        wait();
      }
      catch(InterruptedException e) {}
    }
  }

  synchronized public void resume() {
    available=true;
    notifyAll();
  }
  */
}