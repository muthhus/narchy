package nars.op.software.prolog;


import nars.op.software.prolog.builtins.Builtins;
import nars.op.software.prolog.fluents.DataBase;
import nars.op.software.prolog.io.IO;
import nars.op.software.prolog.terms.*;

import static nars.op.software.prolog.terms.Prog.firstSolution;

/**
  Initializes Prolog. Sets up shared data areas.
  Ensures that lib.class, obtained from lib.pro->lib.java is loaded.
*/
public class Prolog {

//  public static final int version=101;
//  public static final String getInfo() {
//    String s="Kernel Prolog "+version/100.0+ '\n'
//        +"Copyright (c) Paul Tarau 1999-2011\n";
//    return s;
//  }
  
  public static final String default_lib= "prolog/lib.prolog";
  
  public final DataBase db;

  public Builtins dict;
  
  public Clause goal(String line) {
    return Clause.goalFromString(this, line);
  }
  
  public void query(String query) {
      timeGoal(goal(query));
  }
  
  /**
  * reads a query from input strea
  */
  Clause goal() {
    return goal(IO.promptln("?- "));
  }
  
  /**
  * evalutes a query
  */
  public void eval(Clause goal) {
    Clause NamedGoal=goal.cnumbervars(false);
    Term Names=NamedGoal.head();
    if(!(Names instanceof Fun)) { // no vars in Goal
      Term Result= firstSolution(this, goal.head(),goal.body());
      if(!Const.aNo.equals(Result))
        Result=Const.aYes;
      IO.println(Result.toString());
      return;
    }
    
    Prog E=new Prog(this, goal,null);
    
    for(int i=0;;i++) {
      Term R=Prog.ask_engine(E);
      // IO.mes("GOAL:"+Goal+"\nANSWER: "+R);
      if(R==null) {
        IO.println("no");
        break;
      }
      Fun NamedR=(Fun)R.numbervars();
      for(int j = 0; j<Names.arity(); j++) {
        IO.println(((Fun)Names).arg(j)+"="+NamedR.arg(j));
      }
      // IO.println(";");
      if(!moreAnswers(i)) {
        E.stop();
        break;
      }
    }
  }
  
  static boolean moreAnswers(int i) {
    if(IO.maxAnswers==0) { // under user control
      String more=IO.promptln("; for more, <enter> to stop: ");
      return more.equals(";");
    } else if(i<IO.maxAnswers||IO.maxAnswers<0) {
      IO.println(";"); // print all remaining
      return true;
    } else { // i >= ...}
      IO.println(";");
      IO.println("No more answers computed, max reached! ("+IO.maxAnswers+ ')');
      return false;
    }
  }
  
  /**
  *  evaluates and times a Goal querying program P
  */
  
  public void timeGoal(Clause goal) {
    long t1=System.currentTimeMillis();
    try {
      eval(goal);
    } catch(Throwable e) {
      IO.error("Execution error in goal:\n  "+goal.pprint()+".\n",e);
    }
    long t2=System.currentTimeMillis();
    IO.println("Time: "+(t2-t1)/1000.0+" sec");
  }
  
  /**
  *  (almost) standard Prolog-like toplevel in Java
  *  (will) print out variables and values
  */
  public void standardTop() {
    standardTop("?- ");
  }
  
  public void standardTop(String prompt) {
    for(;;) {
      Clause G= goal(IO.promptln(prompt));
      if(null==G) {
        continue;
      }
      IO.peer=null;
      timeGoal(G);
    }
  }
  
  /**
   Asks Prolog a query Answer, Goal and returns the
   first solution of the form "the(Answer)" or the constant
   "no" if no solution exists
  */
  public Term ask(Term answer, Term body) {
    return firstSolution(this, answer,body);
  }
  
  /**
    Asks Prolog a query Goal and returns the
    first solution of the form "the(Answer)" , where
    Answer is an instance of Goal or the constant
    "no" if no solution exists
  */
  public Term ask(Term goal) {
    return ask(goal,goal);
  }
  
  /**
    Asks Prolog a String query and gets back a string Answer
    of the form "the('[]'(VarsOfQuery))" containing a binding
    of the variables or the first solution to the query or "no"
    if no such solution exists
  */
  public String ask(String query) {
    Clause Goal= goal(query);
    Term Body=Goal.body();
    return ask(Body).pprint();
  }
  
  public Prolog run(String[] args) {
    if(null!=args) {
      for(int i=0;i<args.length;i++) {
        String result= ask(args[i]);
        IO.trace(result);
        if("no".equals(result.intern())) {
          IO.error("failing cmd line argument: "+args[i]);
          return null;
        }
      }
    }
    return this;
  }

  public Const toConstBuiltin(Const c) {
    //TODO switch
    if(c.name().equals(Const.aNil.name()))
      return Const.aNil;
    if(c.name().equals(Const.aNo.name()))
      return Const.aNo;
    if(c.name().equals(Const.aYes.name()))
      return Const.aYes;

    ConstBuiltin B=(ConstBuiltin) dict.the(c);
    if(null==B) {
      // IO.mes("not a builtin:"+this);
      return c;
    }
    return B;
  }

//  /**
//     Initialises key data areas. Runs a first query, which,
//     if suceeeds a true, otherwise false is returned
//  */
//  public static final boolean startProlog() {
//    // should be final for expiration mechanism (it should avoid overriding!)
//    //IO.println(getInfo());
//
//
//    return true;
//  }

  public Prolog() {
    db =new DataBase();
  }
  
}
