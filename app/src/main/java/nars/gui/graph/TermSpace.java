package nars.gui.graph;

import jcog.memoize.LinkedMRUMemoize;
import jcog.memoize.Memoize;
import nars.concept.Concept;
import nars.gui.DynamicListSpace;
import nars.term.Termed;
import org.eclipse.collections.api.tuple.Pair;

import java.util.function.Function;


abstract public class TermSpace<X extends Termed> extends DynamicListSpace<X,TermWidget<X>> {

}
