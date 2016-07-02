package nars.nal.op;

import nars.term.Compound;
import nars.term.Term;


public interface TermTransform {

    Term function(Compound args);

}
