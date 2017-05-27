package nars.gui.graph;

import jcog.bag.impl.hijack.HijackMemoize;
import jcog.pri.PLink;
import nars.NAR;
import nars.concept.Concept;
import nars.control.ConceptFire;
import nars.gui.NARSpace;
import nars.term.Term;
import org.eclipse.collections.api.tuple.Pair;


abstract public class ConceptSpace extends NARSpace<Term, ConceptWidget> {


    public final NAR nar;

    public final int maxEdgesPerNodeMin, maxEdgesPerNodeMax;

    public long now;
    public int dur;

    protected final ConceptWidget.ConceptVis conceptVis = new ConceptWidget.ConceptVis2();

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

    public final HijackMemoize<Pair<Concept, ConceptWidget /* target */>, ConceptWidget.TermEdge> edges = new HijackMemoize<>(4096, 2, (to) -> {
        return new ConceptWidget.TermEdge(to.getTwo());
    });

    void removeNode(ConceptFire concept) {
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


    protected ConceptWidget nodeGetOrCreate(PLink<ConceptFire> clink) {
        ConceptWidget cw = nodeGetOrCreate(clink.get());
        cw.pri = clink.priSafe(0);
        return cw;
    }

    protected ConceptWidget nodeGetOrCreate(ConceptFire concept) {
        ConceptWidget cw = space.getOrAdd(concept.term(), (c) -> {
            return new ConceptWidget(c);
        });
        cw.concept = concept.get();
        cw.activate();
        return cw;
    }


    @Override
    protected void update() {
        super.update();

        for (int i = 0, activeSize = active.size(); i < activeSize; i++)
            active.get(i).commit(conceptVis, this);
    }


}
