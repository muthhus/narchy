package nars.op.software.prolog.builtins;

import nars.op.software.prolog.Init;
import nars.op.software.prolog.fluents.*;
import nars.op.software.prolog.io.*;
import nars.op.software.prolog.terms.*;

/**
 This class contains a dictionary of all builtins i.e.
 Java based classes callable from Prolog.
 They should provide a constructor and an exec method.
 @author Paul Tarau
*/
public class Builtins extends HashDict {
  
  /**
     This constructor registers builtins. Please put a header here
     if you add a builtin at the bottom of this file.
  */
  public Builtins(){
    // add a line here for each new builtin
    
    // basics
    register(new is_builtin());
    register(Const.aTrue);
    register(Const.aFail);
    register(new halt());
    register(new compute());
    
    // I/O and trace related
    register(new get_stdin());
    register(new get_stdout());
    register(new set_max_answers());
    register(new set_trace());
    register(new stack_dump());
    register(new consult());
    register(new reconsult());
    register(new reconsult_again());
    
    // database
    register(new at_key());
    register(new pred_to_string());
    register(new db_to_string());
    
    register(new new_db());
    register(new get_default_db());
    register(new db_remove());
    register(new db_add());
    register(new db_collect());
    register(new db_source());
    
    // data structure builders/converters
    register(new arg());
    register(new new_fun());
    register(new get_arity());
    register(new name_to_chars());
    register(new chars_to_name());
    register(new numbervars());
    
    // fluent constructors
    register(new unfolder_source());
    register(new answer_source());
    
    register(new source_list());
    register(new list_source());
    
    register(new term_source());
    register(new source_term());
    
    register(new integer_source());
    register(new source_loop());
    
    // Fluent Modifiers
    
    register(new set_persistent());
    register(new get_persistent());
    
    // Input Sources
    register(new file_char_reader());
    register(new char_file_writer());
    
    register(new file_clause_reader());
    register(new clause_file_writer());
    
    // writable Sinks
    register(new term_string_collector());
    register(new term_collector());
    
    register(new string_char_reader());
    register(new string_clause_reader());
    
    // fluent controllers
    register(new get());
    register(new put());
    register(new stop());
    register(new collect());
    
    // fluent combinators
    register(new split_source());
    register(new merge_sources());
    // see compose_sources,append_sources,merge_sources in lib.pro
    
    // discharges a Source to a Sink
    register(new discharge());
    
    // OS and process interface
    register(new system());
    register(new ctime());
  }
  
  /**
    registers a symbol as name of a builtin
  */
  public void register(Const proto) {
    String key=proto.name()+ '/' +proto.arity();
    // IO.mes("registering builtin: "+key);
    put(key,proto);
  }
  
  /**
    Creates a new builtin
  */
  public Const newBuiltin(Const S) {
    String className=S.name();
    int arity=S.arity();
    String key=className+ '/' +arity;
    Const b=(Const)get(key);
    return b;
  }
  
  public static Const toConstBuiltin(Const c) {
    if(c.name().equals(Const.aNil.name()))
      return Const.aNil;
    if(c.name().equals(Const.aNo.name()))
      return Const.aNo;
    if(c.name().equals(Const.aYes.name()))
      return Const.aYes;
    
    ConstBuiltin B=(ConstBuiltin)Init.builtinDict.newBuiltin(c);
    if(null==B) {
      // IO.mes("not a builtin:"+this);
      return c;
    }
    return B;
  }
  
  public static Fun toFunBuiltin(Fun f) {
    if(f.name().equals(":-")&&f.arity()==2) {
      return new Clause(f.args[0],f.args[1]);
    }
    if(f.name().equals(",")&&f.arity()==2) {
      return new Conj(f.args[0],f.args[1]);
    }
    FunBuiltin B=(FunBuiltin)Init.builtinDict.newBuiltin(f);
    if(null==B)
      return f;
    B=(FunBuiltin)B.funClone();
    B.args=f.args;
    return B;
  }
  
} // end Builtins

// Code for actual kernel Builtins:
// add your own builtins in UserBuiltins.java, by cloning the closest example:-)

/**
 * checks if something is a builtin
 */
final class is_builtin extends FunBuiltin {
  is_builtin(){
    super("is_builtin",1);
  }
  
  public int exec(Prog p) {
    return arg(0).isBuiltin()?1:0;
  }
}

/**
  does its best to halt the program:-)
*/
final class halt extends ConstBuiltin {
  halt(){
    super("halt");
  }
  
  public int exec(Prog p) {
    Runtime.getRuntime().exit(0);
    return 1;
  }
}

/**
 * Calls an external program
 */
final class system extends FunBuiltin {
  system(){
    super("system",1);
  }
  
  public int exec(Prog p) {
    String cmd=((Const) arg(0)).name();
    return IO.system(cmd);
  }
}

/**
  opens a reader returning the content of a file char by char
*/
final class file_char_reader extends FunBuiltin {
  file_char_reader(){
    super("file_char_reader",2);
  }
  
  public int exec(Prog p) {
    Term I= arg(0);
    Fluent f;
    if(I instanceof CharReader)
      f=new CharReader(((CharReader)I).reader,p);
    else {
      String s=((Const)I).name();
      f=new CharReader(s,p);
    }
    return putArg(1,f,p);
  }
}

/**
  opens a reader returning clauses from a file
*/
final class file_clause_reader extends FunBuiltin {
  file_clause_reader(){
    super("file_clause_reader",2);
  }
  
  public int exec(Prog p) {
    Term I= arg(0);
    Fluent f;
    if(I instanceof CharReader)
      f=new ClauseReader(((CharReader)I).reader,p);
    else {
      String s=((Const) arg(0)).name();
      f=new ClauseReader(s,p);
    }
    return putArg(1,f,p);
  }
}

/**
  opens a writer which puts characters to a file one by one
*/
final class char_file_writer extends FunBuiltin {
  char_file_writer(){
    super("char_file_writer",2);
  }
  
  public int exec(Prog p) {
    String s=((Const) arg(0)).name();
    Fluent f=new CharWriter(s,p);
    return putArg(1,f,p);
  }
}

/**
  opens a writer which puts characters to a file one by one
*/
final class clause_file_writer extends FunBuiltin {
  clause_file_writer(){
    super("clause_file_writer",2);
  }
  
  public int exec(Prog p) {
    String s=((Const) arg(0)).name();
    Fluent f=new ClauseWriter(s,p);
    return putArg(1,f,p);
  }
}

/**
  get the standard output (a reader)
*/
final class get_stdin extends FunBuiltin {
  get_stdin(){
    super("get_stdin",1);
  }
  
  public int exec(Prog p) {
    return putArg(0,new ClauseReader(p),p);
  }
}

/**
  get standard output (a writer)
*/
final class get_stdout extends FunBuiltin {
  get_stdout(){
    super("get_stdout",1);
  }
  
  public int exec(Prog p) {
    return putArg(0,new ClauseWriter(p),p);
  }
}

/**
  gets an arity for any term:
  n>0 for f(A1,...,An)
  0 for a constant like a
  -1 for a variable like X
  -2 for an integer like 13
  -3 for real like 3.14
  -4 for a wrapped JavaObject;
  @see Term#arity
*/
final class get_arity extends FunBuiltin {
  get_arity(){
    super("get_arity",2);
  }
  
  public int exec(Prog p) {
    Int N=new Int(arg(0).arity());
    return putArg(1,N,p);
  }
}

/**
 * Dumps the current Java Stack
 */
final class stack_dump extends FunBuiltin {
  
  stack_dump(){
    super("stack_dump",1);
  }
  
  public int exec(Prog p) {
    String s= arg(0).toString();
    IO.error("User requested dump",(new Exception(s)));
    return 1;
  }
}

/**
  returns the real time spent up to now
*/
final class ctime extends FunBuiltin {
  
  ctime(){
    super("ctime",1);
  }
  
  private final static long t0=System.currentTimeMillis();
  
  public int exec(Prog p) {
    Term T=new Int(System.currentTimeMillis()-t0);
    return putArg(0,T,p);
  }
}

/**
  sets max answer counter for toplevel query
  if == 0, it will prompt the user for more answers 
  if > 0 it will not print more than IO.maxAnswers
  if < 0 it will print them out all
*/
final class set_max_answers extends FunBuiltin {
  set_max_answers(){
    super("set_max_answers",1);
  }
  
  public int exec(Prog p) {
    IO.maxAnswers=getIntArg(0);
    return 1;
  }
}

/**
  reconsults a file of clauses while overwriting old predicate
  definitions
  @see consult

*/

final class reconsult extends FunBuiltin {
  reconsult(){
    super("reconsult",1);
  }
  
  public int exec(Prog p) {
    String f=((Const) arg(0)).name();
    return DataBase.fromFile(f)?1:0;
  }
}

/**
  consults a file of clauses while adding clauses to
  existing predicate definitions
  @see reconsult
*/
final class consult extends FunBuiltin {
  consult(){
    super("consult",1);
  }
  
  public int exec(Prog p) {
    String f=((Const) arg(0)).name();
    if (IO.trace()) IO.trace("consulting: "+f);
    return DataBase.fromFile(f,false)?1:0;
  }
}

/**
  shorthand for reconsulting the last file
*/
final class reconsult_again extends ConstBuiltin {
  reconsult_again(){
    super("reconsult_again");
  }
  
  public int exec(Prog p) {
    return DataBase.fromFile()?1:0;
  }
}

/**
 * gets default database
 */
final class get_default_db extends FunBuiltin {
  get_default_db(){
    super("get_default_db",1);
  }
  
  public int exec(Prog p) {
    return putArg(0,new JavaObject(Init.default_db),p);
  }
}

// databse operations

/**
 * creates new database
 */
final class new_db extends FunBuiltin {
  new_db(){
    super("new_db",1);
  }
  
  public int exec(Prog p) {
    return putArg(0,new JavaObject(new DataBase()),p);
  }
}

/**
  Puts a term on the local blackboard
*/
final class db_add extends FunBuiltin {
  
  db_add(){
    super("db_add",2);
  }
  
  public int exec(Prog p) {
    DataBase db=(DataBase) arg(0).toObject();
    Term X= arg(1);
    // IO.mes("X==>"+X);
    String key=X.getKey();
    // IO.mes("key==>"+key);
    if(null==key)
      return 0;
    db.out(key,X);
    // IO.mes("res==>"+R);
    return 1;
  }
}

/**
  removes a matching term if available, fails otherwise
*/
final class db_remove extends FunBuiltin {
  
  db_remove(){
    super("db_remove",3);
  }
  
  public int exec(Prog p) {
    DataBase db=(DataBase) arg(0).toObject();
    Term X= arg(1);
    Term R=db.cin(X.getKey(),X);
    return putArg(2,R,p);
  }
}

/**
  collects all matching terms in a (possibly empty) list
  @see out
  @see in
*/
final class db_collect extends FunBuiltin {
  
  db_collect(){
    super("db_collect",3);
  }
  
  public int exec(Prog p) {
    DataBase db=(DataBase) arg(0).toObject();
    Term X= arg(1);
    Term R=db.all(X.getKey(),X);
    return putArg(2,R,p);
  }
}

/**
 * Maps a DataBase to a Source enumerating its elements
 */
final class db_source extends FunBuiltin {
  
  db_source(){
    super("db_source",2);
  }
  
  public int exec(Prog p) {
    DataBase db=(DataBase) arg(0).toObject();
    Source S=new JavaSource(db.toEnumeration(),p);
    return putArg(1,S,p);
  }
}

/**
  collects all matching terms in a (possibly empty) list
*/
final class at_key extends FunBuiltin {
  
  at_key(){
    super("at_key",2);
  }
  
  public int exec(Prog p) {
    Term R=Init.default_db.all(arg(0).getKey(),new Var());
    return putArg(1,R,p);
  }
}

/**
 * Returns a representation of predicate as a string constant
 */
final class pred_to_string extends FunBuiltin {
  
  pred_to_string(){
    super("pred_to_string",2);
  }
  
  public int exec(Prog p) {
    String key= arg(0).getKey();
    String listing=Init.default_db.pred_to_string(key);
    if(null==listing)
      return 0;
    Const R=new Const(listing);
    return putArg(1,R,p);
  }
}

/**
  lists all the local blackboard to a string (Linda terms + clauses)
*/
final class db_to_string extends FunBuiltin {
  db_to_string(){
    super("db_to_string",1);
  }
  
  public int exec(Prog p) {
    return putArg(0,new Const(Init.default_db.pprint()),p);
  }
}

/**
  arg(I,Term,X) unifies X with the I-the argument of functor T
*/
final class arg extends FunBuiltin {
  arg(){
    super("arg",3);
  }
  
  public int exec(Prog p) {
    int i=getIntArg(0);
    Fun F=(Fun) arg(1);
    Term A=(i==0)?new Const(F.name()):((i==-1)?new Int(F.arity())
        :F.args[i-1]);
    return putArg(2,A,p);
  }
}

/**
  new_fun(F,N,T) creates a term T based on functor F with arity
  N and new free varables as arguments
*/
final class new_fun extends FunBuiltin {
  new_fun(){
    super("new_fun",3);
  }
  
  public int exec(Prog p) {
    String s=((Const) arg(0)).name();
    int i=getIntArg(1);
    Term T;
    if(i==0)
      T=Builtins.toConstBuiltin(new Const(s));
    else {
      Fun F=new Fun(s);
      F.init(i);
      T=Builtins.toFunBuiltin(F);
    }
    return putArg(2,T,p);
  }
}

/**
  converts a name to a list of chars
*/
final class name_to_chars extends FunBuiltin {
  name_to_chars(){
    super("name_to_chars",2);
  }
  
  public int exec(Prog p) {
    Term Cs= arg(0).toChars();
    return putArg(1,Cs,p);
  }
}

/**
  converts a name to a list of chars
*/
final class chars_to_name extends FunBuiltin {
  chars_to_name(){
    super("chars_to_name",3);
  }
  
  public int exec(Prog p) {
    int convert=getIntArg(0);
    String s=charsToString((Nonvar) arg(1));
    Nonvar T=new Const(s);
    if(convert>0) {
      try {
        double r= Double.valueOf(s);
        if(Math.floor(r)==r)
          T=new Int((long)r);
        else
          T=new Real(r);
      } catch(NumberFormatException e) {
      }
    }
    return putArg(2,T,p);
  }
}

/**
 * returns a copy of a Term with variables uniformly replaced with 
 * constants 
 */
final class numbervars extends FunBuiltin {
  numbervars(){
    super("numbervars",2);
  }
  
  public int exec(Prog p) {
    Term T= arg(0).numbervars();
    return putArg(1,T,p);
  }
}

/**
 * Performs simple arithmetic operations
 * like compute('+',1,2,Result)
 */
final class compute extends FunBuiltin {
  compute(){
    super("compute",4);
  }
  
  public int exec(Prog p) {
    
    Term o= arg(0);
    Term a= arg(1);
    Term b= arg(2);
    if(!(o instanceof Const)||!(a instanceof Num)||!(b instanceof Num))
      IO.error("bad arithmetic operation ("+o+"): "+a+ ',' +b+"\nprog: "
          +p.toString());
    String opname=((Const)o).name();
    double x=((Num)a).getValue();
    double y=((Num)b).getValue();
    double r;
    char op=opname.charAt(0);
    switch(op) {
      case '+':
        r=x+y;
      break;
      case '-':
        r=x-y;
      break;
      case '*':
        r=x*y;
      break;
      case '/':
        r=x/y;
      break;
      case ':':
        r=(int)(x/y);
      break;
      case '%':
        r=x%y;
      break;
      case '?':
        r=(x<y)?(-1):(x==y?0:1);
      break; // compares!
      case 'p':
        r=Math.pow(x,y);
      break;
      case 'l':
        r=Math.log(y)/Math.log(x);
      break;
      case 'r':
        r=x*Math.random()+y;
      break;
      case '<':
        r=(long)x<<(long)y;
      break;
      case '>':
        r=(long)x>>(long)y;
      break;
      
      default:
        IO.error("bad arithmetic operation <"+op+"> on "+x+" and "+y);
        return 0;
    }
    Num R=((Math.floor(r)==r))? new Int((long)r) : new Real(r);
    return putArg(3,R,p);
  }
}

/**
 * controls trace levels for debugging
 */
final class set_trace extends FunBuiltin {
  set_trace(){
    super("set_trace",1);
  }
  
  public int exec(Prog p) {
    Prog.tracing=getIntArg(0);
    return 1;
  }
}

/**
  Explores a finite iterator and return its
  successive values as a list.
*/

final class source_list extends FunBuiltin {
  source_list(){
    super("source_list",2);
  }
  
  public int exec(Prog p) {
    Source S=(Source) arg(0);
    Term Xs=S.toList();
    return putArg(1,Xs,p);
  }
}

/**
 * maps a List to a Source
 */
final class list_source extends FunBuiltin {
  
  list_source(){
    super("list_source",2);
  }
  
  public int exec(Prog p) {
    Source E=new ListSource((Const) arg(0),p);
    return putArg(1,E,p);
  }
}

/**
 * maps a Term to a Source
 */
final class term_source extends FunBuiltin {
  
  term_source(){
    super("term_source",2);
  }
  
  public int exec(Prog p) {
    TermSource E=new TermSource((Nonvar) arg(0),p);
    return putArg(1,E,p);
  }
}

/**
 * Creates an Integer Source which advances at most Fuel (infinite if Fule==0) 
 * Steps computing each time x:= a*x+b. Called as: integer_source(Fuel,A,X,B,NewSource)
 */
final class integer_source extends FunBuiltin {
  
  integer_source(){
    super("integer_source",5);
  }
  
  public int exec(Prog p) {
    IntegerSource E=new IntegerSource(((Int) arg(0)).longValue(),
        ((Int) arg(1)).longValue(),((Int) arg(2)).longValue(),
        ((Int) arg(3)).longValue(),p);
    return putArg(4,E,p);
  }
}

/**
  Builds a Looping Source from a Source.
*/
final class source_loop extends FunBuiltin {
  source_loop(){
    super("source_loop",2);
  }
  
  public int exec(Prog p) {
    Source s=(Source) arg(0);
    return putArg(1,new SourceLoop(s,p),p);
  }
}

/**
 * Builds a Source from a Term
 */
final class source_term extends FunBuiltin {
  
  source_term(){
    super("source_term",2);
  }
  
  public int exec(Prog p) {
    Source S=(Source) arg(0);
    Term Xs=Builtins.toFunBuiltin(((Fun)S.toFun()));
    return putArg(1,Xs,p);
  }
}

// Solvers and iterators over clauses

/**
 * When called as answer_source(X,G,R), it
   builds a new clause and maps it to an AnswerSource 
   LD-resolution interpreter which will return one answer 
   at a time of the form "the(X)" using G as initial resolvent
   and "no" when no more answers are available.
*/
final class answer_source extends FunBuiltin {
  answer_source(){
    super("answer_source",3);
  }
  
  public int exec(Prog p) {
    Clause goal=new Clause(arg(0), arg(1));
    Prog U=new Prog(goal,p);
    return putArg(2,U,p);
  }
}

/**
  Builds a new clause H:-B and maps it to an iterator
*/
final class unfolder_source extends FunBuiltin {
  unfolder_source(){
    super("unfolder_source",2);
  }
  
  public int exec(Prog p) {
    Clause goal= arg(0).toClause();
    Prog newp=new Prog(goal,p);
    Unfolder S=new Unfolder(goal,newp);
    return putArg(1,S,p);
  }
}

/**
 generic Source advancement step,
 similar to an iterator's nextElement operation,
 gets one element from the Source
*/

final class get extends FunBuiltin {
  get(){
    super("get",2);
  }
  
  public int exec(Prog p) {
    // IO.mes("<<"+getArg(0)+"\n"+p+p.getTrail().pprint());
    Source S=(Source) arg(0);
    Term A=Const.the(S.getElement());
    // if(null==A) A=Const.aNo;
    // else A=new Fun("the",A);
    // IO.mes(">>"+A+"\n"+p+p.getTrail().pprint());
    return putArg(1,A,p);
  }
}

/**
 generic Sink advancement step,
 sends one element to the Sink 
*/

final class put extends FunBuiltin {
  put(){
    super("put",2);
  }
  
  public int exec(Prog p) {
    Sink S=(Sink) arg(0);
    Term X= arg(1);
    if(0==S.putElement(X)) {
      IO.error("error in putElement: "+X);
    }
    return 1;
  }
}

/**
  frees a Fluent's resources and ensures
  it cannot produce/consume any new values
*/
final class stop extends FunBuiltin {
  stop(){
    super("stop",1);
  }
  
  public int exec(Prog p) {
    Fluent S=(Fluent) arg(0);
    S.stop();
    return 1;
  }
}

/**
  Splits a (finite) Source in two new ones
  which inherit the current state of the parent.
*/
final class split_source extends FunBuiltin {
  split_source(){
    super("split_source",3);
  }
  
  public int exec(Prog p) {
    Source original=(Source) arg(0);
    Const Xs=original.toList();
    return (putArg(1,new ListSource(Xs,p),p)>0&&putArg(2,new ListSource(Xs,p),p)>0)?1
        :0;
  }
}

/**
  Merges all Sources contained in a List into one Source.
*/
final class merge_sources extends FunBuiltin {
  merge_sources(){
    super("merge_sources",2);
  }
  
  public int exec(Prog p) {
    Const list=(Const) arg(0);
    return putArg(1,new SourceMerger(list,p),p);
  }
}

/**
  Flushes to a Sink the content of a Source Fluent
*/
final class discharge extends FunBuiltin {
  discharge(){
    super("discharge",2);
  }
  
  public int exec(Prog p) {
    Source from=(Source) arg(0);
    Sink to=(Sink) arg(1);
    for(;;) {
      Term X=from.getElement();
      if(null==X) {
        to.stop();
        break;
      } else
        to.putElement(X);
    }
    return 1;
  }
}

/**
  Collects a reference to or the content of a Sink
*/

final class collect extends FunBuiltin {
  collect(){
    super("collect",2);
  }
  
  public int exec(Prog p) {
    Sink s=(Sink) arg(0);
    Term X=s.collect();
    if(null==X)
      X=Const.aNo;
    else
      X=new Fun("the",X);
    return putArg(1,X,p);
  }
}

/**
 * Builds a StringSink which concatenates String representations
 * of Terms with put/1 and the return their concatenation with collect/1
 */
final class term_string_collector extends FunBuiltin {
  term_string_collector(){
    super("term_string_collector",1);
  }
  
  public int exec(Prog p) {
    return putArg(0,new StringSink(p),p);
  }
}

/**
 * Builds a TermCollector Sink which accumulates
 * Terms with put/1 and the return them with collect/1
 */
final class term_collector extends FunBuiltin {
  term_collector(){
    super("term_collector",1);
  }
  
  public int exec(Prog p) {
    return putArg(0,new TermCollector(p),p);
  }
}

/**
 * Creates a char reader from a String.
 */
final class string_char_reader extends FunBuiltin {
  string_char_reader(){
    super("string_char_reader",2);
  }
  
  public int exec(Prog p) {
    return putArg(1,new CharReader(arg(0),p),p);
  }
}

/**
 *  Creates a clause reader from a String.
 */
final class string_clause_reader extends FunBuiltin {
  string_clause_reader(){
    super("string_clause_reader",2);
  }
  
  public int exec(Prog p) {
    return putArg(1,new ClauseReader(arg(0),p),p);
  }
}

/**
 * set_persistent(Fluent,yes)
 * makes a Fluent persistent - i.e. likely to keep
 * its state on backtracking. This assumes that the
 * Fluent remains accessible by being saved in a Database
 * or as element of a Fluent with longer life span.
 * 
 * set_persistent(Fluent,no) makes the Fluent perish
 * on backtracking (default behavior)
 */
final class set_persistent extends FunBuiltin {
  set_persistent(){
    super("set_persistent",2);
  }
  
  public int exec(Prog p) {
    Fluent F=(Fluent) arg(0);
    Const R=(Const) arg(1);
    boolean yesno=!R.equals(Const.aNo);
    F.setPersistent(yesno);
    return 1;
  }
}

/**
 * Gets the yes/no persistentcy value of a Fluent.
 */
final class get_persistent extends FunBuiltin {
  get_persistent(){
    super("get_persistent",2);
  }
  
  public int exec(Prog p) {
    Fluent F=(Fluent) arg(0);
    Term R=F.getPersistent()?Const.aYes:Const.aNo;
    return putArg(1,R,p);
  }
}
