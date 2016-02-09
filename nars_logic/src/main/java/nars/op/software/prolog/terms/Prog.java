package nars.op.software.prolog.terms;

import nars.op.software.prolog.Prolog;
import org.jetbrains.annotations.NotNull;

/**
  Basic toplevel Prolog Engine. Loads and executes Prolog
  programs and can be extended to spawn threads executing on new Prolog Engine
  objects as well as networking threads and 
  synced local and remote Linda transactions
*/
public class Prog extends Source implements Runnable {
  private final Prolog prolog;
  // CONSTRUCTORS
  
  /**
    Creates a Prog starting execution with argument "goal" 
  */
  public Prog(Prolog prolog, Clause goal, Prog parent){
    super(parent);
    this.prolog = prolog;
    this.parent=parent;
    goal=goal.ccopy();
    this.trail=new Trail();
    this.orStack= new Trail();
    if(null!=goal)
      orStack.add(nextUnfolder(goal));
    
  }
  
  // INSTANCE FIELDS
  
  private Trail trail;
  
  /**
   * Contains Unfolders that may produce answers.
   */
  private Trail orStack;
  
  private final Prog parent;
  
  public final Trail getTrail() {
    return trail;
  }
  
  public final Prog getParent() {
    return parent;
  }
  
  // CLASS FIELDS
  
  public static int tracing=1;
  
  // INSTANCE METHODS
  
  /**
   * Here is where actual LD-resolution computation happens.
   * It consists of a chain of "unfolding" steps, possibly
   * involving backtracking, which is managed by the OrStack.
   */
  public Term getElement() {
    if(null==orStack)
      return null;
    Clause answer=null;
    while(!orStack.isEmpty()) {
      Unfolder I=(Unfolder) orStack.removeLast();
      answer=I.getAnswer();
      if(null!=answer)
        break;
      Clause nextgoal=I.getElement();
      if(null!=nextgoal) {
        if(I.notLastClause())
          orStack.add(I);
        else
          I.stop();
        orStack.add(nextUnfolder(nextgoal));
      }
    }
    Term head;
    if(null==answer) {
      head=null;
      stop();
    } else
      head=answer.head();
    return head;
  }

  @NotNull
  private Unfolder nextUnfolder(Clause nextgoal) {
    return new Unfolder(prolog, nextgoal, this);
  }

  public void stop() {
    if(null!=trail) {
      trail.unwind(0);
      trail=null;
    }
    orStack=null;
  }
  
  /** 
    Computes a copy of the first solution X of Goal G.
  */
  
  public static Term firstSolution(Prolog prolog, Term X,Term G) {
    Prog p= new Prog(prolog, new Clause(X, G), null);
    Term A=ask_engine(p);
    if(A!=null) {
      A=new Fun("the",A);
      p.stop();
    } else
      A=Const.aNo;
    return A;
  }

  /** asks a logic engine to return a solution
   */
  
  static public Term ask_engine(Prog p) {
    return p.getElement();
  }
  
  /** 
   * usable for launching on a separate thread
   */
  public void run() {
    for(;;) {
      Term Answer=getElement();
      if(null==Answer)
        break;
    }
  }
}