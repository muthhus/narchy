package nars.op.software.prolog;


import nars.op.software.prolog.builtins.Builtins;

/**
   Minimal command line only Prolog main entry point
*/
public class PrologMain {
  public static int init() {
    if(!Init.startProlog())
      return 0;
    Init.builtinDict=new Builtins();
    Init.askProlog("reconsult('"+
            PrologMain.class.getResource(Init.default_lib).toExternalForm()
            +"')");
    return 1;
  }
  
  public static void main(String args[]) {
    if(0==init())
      return;
    if(!Init.run(args))
      return;
    Init.standardTop(); // interactive
  }
}
