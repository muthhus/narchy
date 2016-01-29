package nars.op.software.prolog.terms;

import prolog.fluents.HashDict;

/**
  Used in implementing uniform replacement of
  variables with new constants. useful for printing
  out with nicer variable names.

  @see Var
  @see Clause
*/
class VarNumberer extends SystemObject {
  final HashDict dict;
  
  int ctr;
  
  VarNumberer(){
    dict=new HashDict();
    ctr=0;
  }
  
  Term action(Term place) {
    place=place.ref();
    // IO.trace(">>action: "+place);
    if(place instanceof Var) {
      Const root=(Const)dict.get(place);
      if(null==root) {
        root=new PseudoVar(ctr++);
        dict.put(place,root);
      }
      place=root;
    }
    // IO.trace("<<action: "+place);
    return place;
  }
}
