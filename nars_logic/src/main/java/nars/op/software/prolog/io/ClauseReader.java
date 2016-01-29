package nars.op.software.prolog.io;

import nars.op.software.prolog.terms.*;

import java.io.IOException;
import java.io.Reader;

/**
  Builds  Fluents from Java
  Streams
*/
public class ClauseReader extends CharReader {
  protected Parser parser;
  
  public ClauseReader(Reader reader,Prog p){
    super(reader,p);
    make_parser("from shared reader");
  }
  
  public ClauseReader(String f,Prog p){
    super(f,p);
    make_parser(f);
  }
  
  public ClauseReader(Prog p){
    super(p);
    make_parser("standard input");
  }
  
  /**
   * parses from a string representation of a term
   */
  public ClauseReader(Term t,Prog p){
    super(t,p);
    make_parser("string parser");
  }
  
  void make_parser(String f) {
    if(null!=reader)
      try {
        this.parser=new Parser(reader);
      } catch(IOException e) {
        IO.error("unable to build parser for: "+f);
      }
    else
      this.parser=null;
  }
  
  public Term getElement() {
    Clause C=null;
    if(// IO.peer!=null &&
    reader.equals(IO.input)) {
      String s=IO.promptln(">:");
      if(null==s||0==s.length())
        C=null;
      else
        C=new Clause(s);
    } else if(null!=parser) {
      if(parser.atEOF()) {
        C=null;
        stop();
      } else
        C=parser.readClause();
      if(C!=null&&C.head().equals(Const.anEof)) {
        C=null;
        stop();
      }
    }
    return extract_info(C);
  }
  
  static Fun extract_info(Clause C) {
    if(null==C)
      return null;
    Term Vs=C.varsOf();
    Clause SuperC=new Clause(Vs,C);
    SuperC.dict=C.dict;
    Clause NamedSuperC=SuperC.cnumbervars(false);
    Term Ns=NamedSuperC.head();
    Term NamedC=NamedSuperC.body();
    return new Fun("clause",C,Vs,NamedC,Ns);
  }
  
  public void stop() {
    super.stop();
    parser=null;
  }
}
