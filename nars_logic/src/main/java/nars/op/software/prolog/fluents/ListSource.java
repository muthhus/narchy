package nars.op.software.prolog.fluents;

import prolog.terms.Const;
import prolog.terms.Copier;
import prolog.terms.Prog;

/**
  Builds an iterator from a list
*/
public class ListSource extends JavaSource {
  public ListSource(Const Xs,Prog p){
    super(Copier.ConsToVector(Xs),p);
  }
}
