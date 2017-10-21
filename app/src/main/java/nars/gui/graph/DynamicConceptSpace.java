package nars.gui.graph;

import jcog.bag.util.Bagregate;
import jcog.list.FasterList;
import jcog.pri.PriReference;
import jcog.util.Flip;
import nars.NAR;
import nars.concept.Concept;
import nars.control.Activate;
import nars.control.DurService;
import nars.gui.DynamicListSpace;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DynamicConceptSpace extends DynamicListSpace<Concept, ConceptWidget> {

    public final NAR nar;
    final Bagregate<Activate> concepts;

    private final Flip<List<ConceptWidget>> next = new Flip(FasterList::new);
    final float bagUpdateRate = 0.25f;
    private final int maxNodes;
    private DurService on;


    public TermWidget.TermVis vis;

    public DynamicConceptSpace(NAR nar, @Nullable Iterable<Activate> concepts, int maxNodes, int maxEdgesPerNodeMax) {
        super();
        vis = new ConceptWidget.ConceptVis2(maxNodes * maxEdgesPerNodeMax);
        this.nar = nar;
        this.maxNodes = maxNodes;

        if (concepts == null)
            concepts = (Iterable) this;

        this.concepts = new Bagregate<Activate>(concepts, maxNodes, bagUpdateRate) {

            @Override
            public void onRemove(PriReference<Activate> value) {
                removeNode(value.get());
            }
        };
    }

    void removeNode(Activate concept) {
        if (space != null)
            space.remove(concept.id);

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


    final AtomicBoolean ready = new AtomicBoolean(false);

    @Override
    public void start(SpaceGraph<Concept> space) {
        super.start(space);
        on = DurService.build(nar, () -> {
            if (concepts.update()) {
                ready.set(true);
            }
        });
    }

    @Override
    public synchronized void stop() {
        on.stop();
        on = null;
        super.stop();
    }


    @Override
    protected List<ConceptWidget> get() {

        if (ready.compareAndSet(true, false)) {
            List<ConceptWidget> l = next.write();
            l.clear();
            concepts.forEach((clink) -> {
                ConceptWidget cw = space.getOrAdd(clink.get().id, ConceptWidget::new);
                if (cw != null) {

                    cw.pri = clink.priElseZero();
                    l.add(cw);

                }
                //space.getOrAdd(concept.term(), materializer).setConcept(concept, now)
            });
            l.forEach(c -> c.commit(vis, this));
            vis.update(l);
            next.commit();
        }

        List<ConceptWidget> r = next.read();
        return r;
    }


}
