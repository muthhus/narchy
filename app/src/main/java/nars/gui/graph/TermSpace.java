package nars.gui.graph;

import jcog.map.MRUCache;
import jcog.memoize.LinkedMRUMemoize;
import jcog.memoize.Memoize;
import jcog.pri.PriReference;
import nars.NAR;
import nars.concept.Concept;
import nars.control.Activate;
import nars.gui.NARSpace;
import nars.term.Termed;
import org.eclipse.collections.api.tuple.Pair;

import java.util.Map;
import java.util.function.Function;


abstract public class TermSpace extends NARSpace<Termed, TermWidget> {

    public final NAR nar;

    public final int maxEdgesPerNodeMin, maxEdgesPerNodeMax;

    public long now;
    public int dur;

//    final Map edgeBagSharedMap = new MRUCache(1024);



    public Function<TermWidget, TermWidget.TermEdge> edgeBuilder = TermWidget.TermEdge::new;


//    public ConceptsSpace(NAR nar, int maxNodes, int maxEdgesPerNodeMin, int maxEdgesPerNodeMax) {
//        this(nar, maxNodes, maxNodes, maxEdgesPerNodeMin, maxEdgesPerNodeMax);
//    }

    public TermSpace(NAR nar, int maxEdgesPerNodeMin, int maxEdgesPerNodeMax) {
        super();
        this.nar = nar;
        this.maxEdgesPerNodeMin = maxEdgesPerNodeMin;
        this.maxEdgesPerNodeMax = maxEdgesPerNodeMax;
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


    public final Memoize<Pair<Concept, ConceptWidget /* target */>, ConceptWidget.TermEdge> edges =
            new LinkedMRUMemoize<>((to) -> edgeBuilder.apply(to.getTwo()), 8192);


    void removeNode(Activate concept) {
        if (space != null)
            space.remove(concept.id.term());

//        @Nullable ConceptWidget cw = widgets.getIfPresent(concept.get());
//        if (cw != null) {
//            cw.hide();
//        }
    }
////        cw.delete();
////
////        ConceptWidget cw = concept.remove(this);
////        if (cw!=null) {
////            cw.delete(space.dyn);
////        }
////        return cw;
//    }



    protected ConceptWidget conceptWidget(PriReference<Concept> clink) {
        Concept c = clink.get();
        if (c != null) {

            ConceptWidget cw = space.getOrAdd(c.term(), ConceptWidget.nodeBuilder);
            cw.concept = c;


            cw.activate();
            cw.pri = clink.priElseZero();
            return cw;

        }
        return null;
    }



}
