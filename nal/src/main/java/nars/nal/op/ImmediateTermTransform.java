package nars.nal.op;

import nars.index.TermIndex;
import nars.nal.nal8.operator.TermFunction;
import nars.term.Compound;
import nars.term.Operator;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;


public interface ImmediateTermTransform  {

    Term function(Compound args, TermIndex context);

}
