package nars.gui.graph;

import jcog.memoize.LinkedMRUMemoize;
import jcog.memoize.Memoize;
import nars.concept.Concept;
import nars.gui.DynamicListSpace;
import nars.term.Termed;
import org.eclipse.collections.api.tuple.Pair;

import java.util.function.Function;


abstract public class TermSpace<X extends Termed> extends DynamicListSpace<X,TermWidget<X>> {






//    final Map edgeBagSharedMap = new MRUCache(1024);



    public Function<ConceptWidget, ConceptWidget.ConceptEdge> edgeBuilder = ConceptWidget.ConceptEdge::new;


//    public ConceptsSpace(NAR nar, int maxNodes, int maxEdgesPerNodeMin, int maxEdgesPerNodeMax) {
//        this(nar, maxNodes, maxNodes, maxEdgesPerNodeMin, maxEdgesPerNodeMax);
//    }

    public TermSpace() {
        super();
    }


    //    public final Memoize<Term,ConceptWidget> conceptWidgets = new LinkedMRUMemoize<Term,ConceptWidget>((c) -> {
//        ConceptWidget y = new ConceptWidget(c);
//        return y;
//    }, 2048) {
//        @Override
//        protected void onEvict(Map.Entry<Term, ConceptWidget> entry) {
//            entry.getValue()
//                    .delete(space.dyn);
//        }
//    };

    //conceptWidgets.apply(c);


    public final Memoize<Pair<Concept, ConceptWidget /* target */>, ConceptWidget.ConceptEdge> edges =
            new LinkedMRUMemoize<>((to) -> edgeBuilder.apply(to.getTwo()), 8192);






}
