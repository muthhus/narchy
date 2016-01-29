package nars.op.software.prolog.terms;

/**
  Top element of the Prolog term hierarchy.
  Describes a simple or compound ter like:
  X,a,13,f(X,s(X)),[a,s(X),b,c], a:-b,c(X,X),d, etc.
*/
public abstract class Term extends Object implements Cloneable {
  
  public final static int JAVA=-4;
  
  public final static int REAL=-3;
  
  public final static int INT=-2;
  
  public final static int VAR=-1;
  
  public final static int CONST=0;
  
  /** 
    returns or fakes an arity for all subtypes 
  */
  abstract public int arity();
  
  /** Dereferences if necessary.
      If multi-threaded, this should be synchronized
      otherwise vicious non-reentrancy problems
      may occur in the presence of GC and heavy
      multi-threading!!!
  */
  public Term ref() { // synchronized !!!
    return this;
  }
  
  abstract boolean bind_to(Term that,Trail trail);
  
  /** Unify dereferenced */
  abstract boolean unify_to(Term that,Trail trail);
  
  /** Dereference and unify_to */
  protected final boolean unify(Term that,Trail trail) {
    return ref().unify_to(that.ref(),trail);
  }
  
  protected void undo() { // does nothing
  }
  
  // public abstract boolean eq(Term that);
  
  public Term token() {
    return this;
  }
  
//  Term toTerm() {
//    return this;
//  }
  
  public Clause toClause() {
    return new Clause(this,Const.aTrue);
  }
  
  boolean isClause() {
    return false;
  }
  
  public static Term fromString(String s) {
    return Clause.clauseFromString(s).toTerm();
  }
  
  /**
    Tests if this term unifies with that.
    Bindings are trailed and undone after the test.
    This should be used with the shared term as this
    and the new term as that. Synchronization makes
    sure that side effects on the shared term are
    not interfering, i.e as in:
       SHARED.matches(NONSHARED,trail).

  */
  // synchronized
  public boolean matches(Term that) {
    return matches(that,new Trail());
  }
  
  public boolean matches(Term that,Trail trail) {
    int oldtop=trail.size();
    boolean ok=unify(that,trail);
    trail.unwind(oldtop);
    return ok;
  }
  
  /**
    Returns a copy of the result if the unification
    of this and that. Side effects on this and that
    are undone using trailing of bindings..
    Synchronization happens over this, not over that.
    Make sure it is used as SHARED.matching_copy(NONSHARED,trail).
  */
  // synchronized
  
  public Term matching_copy(Term that) {
    Trail trail=new Trail();
    boolean ok=unify(that,trail);
    // if(ok) that=that.copy();
    if(ok)
      that=copy();
    trail.unwind(0);
    return (ok)?that:null;
  }
  
  /**
    Defines the reaction to an agent recursing
    over the structure of a term. <b>This</b> is passed
    to the agent and the result of the action is returned.
    Through overriding, for instance, a Fun term will provide
    the recursion over its arguments, by applying the action
    to each of them.
    
    @see Fun
  */
  Term reaction(Term agent) {
    Term T=agent.action(this);
    return T;
  }
  
  /**
     Identity action.
  */
  Term action(Term that) {
    return that;
  }
  
  /**
     Returns a copy of a term with variables
     standardized apart (`fresh variables').
  */
  // synchronized
  public Term copy() {
    return reaction(new Copier());
  }
  
  /**
    Returns '[]'(V1,V2,..Vn) where Vi is a variable
    occuring in this Term
  */
  public Term varsOf() {
    return (new Copier()).getMyVars(this);
  }
  
  /**
    Replaces variables with uppercase constants named 
    `V1', 'V2', etc. to be read back as variables.
  */
  public Term numbervars() {
    return copy().reaction(new VarNumberer());
  }
  
  /**
     Prints out a term to a String with
     variables named in order V1, V2,....
  */
  public String pprint() {
    return numbervars().toString();
  }
  
  /*
    Returns an unquoted version of toString()
  */
  public String toUnquoted() {
    return pprint();
  }
  
  /**
     Returns a string key used based on the string name
     of the term. Note that the key for a clause AL-B,C. 
     is the key insted of ':-'.
  */
  public String getKey() {
    return toString();
  }
  
  /** 
    Java Object wrapper. In particular, it is
    used to wrap a Thread to hide it inside a Prolog
    data object.
  */
  public Object toObject() {
    return ref();
  }
  
  /*
    Just to catch the frequent error when the arg is forgotten while
    definig a builtin. Being final, it will generate a compile time 
    error if this happens
  */
  final int exec() {
    
    return -1;
  }
  
  /**
    Executed when a builtin is called.
    Needs to be overriden. Returns a run-time
    warning if this is forgotten.
  */
  
  protected int exec(Prog p) {
    // IO.println("this should be overriden, prog="+p);
    return -1;
  }
  
  static final Nonvar stringToChars(String s) {
    if(0==s.length())
      return Const.aNil;
    Cons l=new Cons(new Int((s.charAt(0))),Const.aNil);
    Cons curr=l;
    for(int i=1;i<s.length();i++) {
      Cons tail=new Cons(new Int((s.charAt(i))),Const.aNil);
      curr.args[1]=tail;
      curr=tail;
    }
    return l;
  }
  
  public Nonvar toChars() {
    return stringToChars(toUnquoted());
  }
  
  /**
    Converts a list of character codes to a String.
  */
  public static String charsToString(Nonvar Cs) {
    StringBuilder s=new StringBuilder("");
    
    while(!(Cs instanceof Nil)) {
      if(!(Cs instanceof Cons))
        return null;
      Nonvar head=(Nonvar)((Cons)Cs).arg(0);
      if(!(head instanceof Int))
        return null;
      char c=(char)((Int)head).val;
      s.append(c);
      Cs=(Nonvar)((Cons)Cs).arg(1);
    }
    
    return s.toString();
  }
  
  public boolean isBuiltin() {
    return false;
  }
}
