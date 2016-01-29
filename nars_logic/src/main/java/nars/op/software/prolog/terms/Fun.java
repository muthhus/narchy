package nars.op.software.prolog.terms;

/**
  Implements compound terms
  @see Term
*/
public class Fun extends Const {
  public Term args[];
  
  public final int arity() {
    return args.length;
  }
  
  public Fun(String s){
    super(s);
    args=null;
  }
  
  /*
  public Fun(int arity) {
     //setDefaultName();
     args=new Term[arity];
  }
  */
  
  public Fun(String name, Term... args){
    super(name);
    this.args=args;
  }
  
  public Fun(String s,int arity){
    super(s);
    args=new Term[arity];
  }
  
  public void init(int arity) {
    Term[] args = this.args = new Term[arity];
    for(int i=0;i<arity;i++) {
      args[i]=new Var();
    }
  }
  
  public final Term arg(int i) {
    return args[i].ref();
  }
  
  public final int getIntArg(int i) {
    return (int)((Int) arg(i)).getValue();
  }
  
  public final void setArg(int i,Term T) {
    args[i]=T;
  }
  
  public final int putArg(int i,Term T,Prog p) {
    // return getArg(i).unify(T,p.getTrail())?1:0;
    return args[i].unify(T,p.getTrail())?1:0;
  }
  
  public Fun(String s,Term x0){
    this(s,1);
    args[0]=x0;
  }
  
  public Fun(String s,Term x0,Term x1){
    this(s,2);
    args[0]=x0;
    args[1]=x1;
  }
  
  public Fun(String s,Term x0,Term x1,Term x2){
    this(s,3);
    args[0]=x0;
    args[1]=x1;
    args[2]=x2;
  }
  
  public Fun(String s,Term x0,Term x1,Term x2,Term x3){
    this(s,4);
    args[0]=x0;
    args[1]=x1;
    args[2]=x2;
    args[3]=x3;
  }
  
  protected final String funToString() {
    if(args==null)
      return qname()+"()";
    int l=args.length;
    return qname()+(l<=0?"": '(' +show_args()+ ')');
  }
  
  public String toString() {
    return funToString();
  }
  
  protected static String watchNull(Term x) {
    return((null==x)?"null":x.toString());
  }
  
  private String show_args() {
    Term[] a = this.args;
    StringBuilder s=new StringBuilder(watchNull(a[0]));
    for(int i = 1; i< a.length; i++) {
      s.append(',').append(watchNull(a[i]));
    }
    return s.toString();
  }
  
  boolean bind_to(Term that,Trail trail) {
    return super.bind_to(that,trail)&&args.length==((Fun)that).args.length;
  }
  
  boolean unify_to(Term that,Trail trail) {
    if(bind_to(that,trail)) {
      Fun other=(Fun)that;
      Term[] a = this.args;
      for(int i = 0; i< a.length; i++) {
        if(!a[i].unify(other.args[i],trail))
          return false;
      }
      return true;
    } else
      return that.bind_to(this,trail);
  }
  
  public Term token() {
    return args[0];
  }
  
  // stuff allowing polymorphic cloning of Fun subclasses
  // without using reflection - should be probaly faster than
  // reflection classes - to check
  
  final public Fun funClone() {
    Fun f=null;
    
    try {
      // use of clone is needed for "polymorphic" copy
      f=(Fun)clone();
    } catch(CloneNotSupportedException e) {
      // IO.errmes("clone: "+e);
    }
    
    return f;
  }
  
  protected Fun unInitializedClone() {
    Fun f=funClone();
    f.args=new Term[args.length];
    return f;
  }
  
  protected Fun initializedClone() {
    Fun f=funClone();
    f.init(args.length);
    return f;
  }
  
  Term reaction(Term that) {
    // IO.mes("TRACE>> "+name());
    Fun f=funClone();
    f.args=new Term[args.length];
    for(int i=0;i<args.length;i++) {
      f.args[i]=args[i].reaction(that);
    }
    return f;
  }
  
  public Const listify() {
    Cons l=new Cons(new Const(name()),Const.aNil);
    Cons curr=l;
    for(int i=0;i<args.length;i++) {
      Cons tail=new Cons(args[i],Const.aNil);
      curr.args[1]=tail;
      curr=tail;
    }
    return l;
  }
  
  boolean isClause() {
    return arity()==2&&name().equals(":-");
  }
}
