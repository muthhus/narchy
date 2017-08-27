package nars.gui.graph;

import jcog.memoize.HijackMemoize;
import jcog.pri.PriReference;
import nars.NAR;
import nars.concept.Concept;
import nars.control.Activate;
import nars.gui.NARSpace;
import nars.term.Term;
import org.eclipse.collections.api.tuple.Pair;

import java.util.function.Function;


abstract public class ConceptSpace extends NARSpace<Term, ConceptWidget> {

    public final NAR nar;

    public final int maxEdgesPerNodeMin, maxEdgesPerNodeMax;

    public long now;
    public int dur;

    public ConceptWidget.ConceptVis vis =
        new ConceptWidget.ConceptVis2();
    public Function<Term, ConceptWidget> nodeBuilder = (c) ->
        new ConceptWidget(c);
    public Function<ConceptWidget, ConceptWidget.TermEdge> edgeBuilder = (to) ->
        new ConceptWidget.TermEdge(to);


//    public ConceptsSpace(NAR nar, int maxNodes, int maxEdgesPerNodeMin, int maxEdgesPerNodeMax) {
//        this(nar, maxNodes, maxNodes, maxEdgesPerNodeMin, maxEdgesPerNodeMax);
//    }

    public ConceptSpace(NAR nar, int maxEdgesPerNodeMin, int maxEdgesPerNodeMax) {
        super();
        this.nar = nar;
        this.maxEdgesPerNodeMin = maxEdgesPerNodeMin;
        this.maxEdgesPerNodeMax = maxEdgesPerNodeMax;
    }

//    public final HijackMemoize<Concept,ConceptWidget> widgets = new HijackMemoize<>(2048, 4, (c) -> {
//        ConceptWidget y = new ConceptWidget(c);
//        y.concept = c;
//        return y;
//    }) {
//        @Override
//        public void onRemoved(@NotNull PLink<Pair<Concept, ConceptWidget>> value) {
//            value.get().getTwo()
//                    //.hide();
//                    .delete(space.dyn);
//        }
//    };



    public final HijackMemoize<Pair<Concept, ConceptWidget /* target */>, ConceptWidget.TermEdge> edges = new HijackMemoize<>((to) -> {
        return edgeBuilder.apply(to.getTwo());
    }, 4096, 2);

    void removeNode(Activate concept) {
        space.remove(concept.term());

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


    protected ConceptWidget nodeGetOrCreate(PriReference<Activate> clink) {
        ConceptWidget cw = nodeGetOrCreate(clink.get());
        cw.pri = clink.priElseZero();
        return cw;
    }

    protected ConceptWidget nodeGetOrCreate(Activate concept) {
        ConceptWidget cw = space.getOrAdd(concept.term(), nodeBuilder);
        cw.concept = concept.get();
        cw.activate();
        return cw;
    }


    protected void update() {
        for (int i = 0, activeSize = active.size(); i < activeSize; i++) {
            ConceptWidget a = active.get(i);
            if (a!=null) a.commit(vis, this);
        }
    }


}
